package im.tox.tox4j.core.callbacks

import java.util.Random

import im.tox.tox4j.core.enums.{ ToxConnection, ToxFileControl, ToxFileKind }
import im.tox.tox4j.testing.autotest.{ AliceBobTest, AliceBobTestBase }

/**
 * This test intends to simulate the situation of resuming a file
 * transmission after deleting and re-adding a friend.
 * - Alice initiated the file transmission and Bob accepted
 * - Alice paused the file transmission after sending 1/10 of the file
 *   and deleted Bob from the friend list
 * - Bob saw Alice went offline and sent a friend request to Alice
 * - Alice accepted the friend request and resumed the file transmission
 * - Bob received all the file data
 */
final class FileResumeAfterRestartTest extends AliceBobTest {

  override type State = Unit
  override def initialState: State = ()

  private val fileData = new Array[Byte](13710)
  private var aliceAddress = Array.ofDim[Byte](0)
  new Random().nextBytes(fileData)

  protected override def newChatClient(name: String, expectedFriendName: String) = new Alice(name, expectedFriendName)

  final class Alice(name: String, expectedFriendName: String) extends ChatClient(name, expectedFriendName) {

    private var aliceOffset = 0L
    private var fileId = Array.ofDim[Byte](0)
    private var aliceSentFileNumber = -1
    private var aliceShouldPause = -1
    private val receivedData = new Array[Byte](fileData.length)
    private var bobSentFileNumber = -1
    private var bobOffset = 0L
    private var selfPublicKey = Array.ofDim[Byte](0)

    override def friendRequest(publicKey: Array[Byte], timeDelta: Int, message: Array[Byte])(state: ChatState): ChatState = {
      assert(isAlice)
      state.addTask { (tox, state) =>
        debug("accept Bob's friend request")
        tox.addFriendNorequest(publicKey)
        aliceShouldPause = 1
        state
      }
    }

    override def friendConnectionStatus(friendNumber: Int, connection: ToxConnection)(state: ChatState): ChatState = {
      if (isAlice) {
        if (connection != ToxConnection.NONE) {
          debug(s"is now connected to friend $friendNumber")
          assert(friendNumber == AliceBobTestBase.FRIEND_NUMBER)
          debug(s"initiate file sending to friend $friendNumber")
          state.addTask { (tox, state) =>
            aliceSentFileNumber = tox.fileSend(friendNumber, ToxFileKind.DATA, fileData.length,
              Array.ofDim[Byte](0), s"file for $expectedFriendName.png".getBytes)
            fileId = tox.getFileFileId(friendNumber, aliceSentFileNumber)
            aliceAddress = tox.getAddress
            state
          }
        } else {
          state
        }
      } else {
        if (connection != ToxConnection.NONE) {
          debug(s"is now connected to friend $friendNumber")
          assert(friendNumber == AliceBobTestBase.FRIEND_NUMBER)
          state
        } else {
          debug("See alice go off-line")
          state.addTask { (tox, state) =>
            tox.deleteFriend(friendNumber)
            tox.addFriend(aliceAddress, "Please add me back".getBytes)
            state
          }
        }
      }
    }

    override def fileChunkRequest(friendNumber: Int, fileNumber: Int, position: Long, length: Int)(state: ChatState): ChatState = {
      assert(isAlice)
      debug(s"got request for ${length}B from $friendNumber for file $fileNumber at $position")
      assert(length >= 0)
      if (length == 0) {
        aliceSentFileNumber = -1
        debug("finish transmission")
        state.finish
      } else {
        if (aliceShouldPause != 0) {
          val nextState = state.addTask { (tox, state) =>
            debug(s"sending $length B to $friendNumber from position $position")
            tox.fileSendChunk(friendNumber, fileNumber, position,
              fileData.slice(position.toInt, Math.min(position.toInt + length, fileData.length)))
            state
          }
          aliceOffset += length
          if (aliceOffset >= fileData.length / 10 && aliceShouldPause == -1) {
            aliceShouldPause = 0
            nextState.addTask { (tox, state) =>
              debug("pause file transmission")
              tox.fileControl(friendNumber, fileNumber, ToxFileControl.PAUSE)
              tox.deleteFriend(friendNumber)
              state
            }
          } else {
            nextState
          }
        } else {
          state
        }
      }
    }

    override def fileRecv(friendNumber: Int, fileNumber: Int, kind: Int, fileSize: Long, filename: Array[Byte])(state: ChatState): ChatState = {
      assert(isBob)
      debug(s"received file send request $fileNumber from friend number $friendNumber current offset $bobOffset")
      assert(friendNumber == AliceBobTestBase.FRIEND_NUMBER)
      assert(kind == ToxFileKind.DATA)
      assert(new String(filename) == s"file for $name.png")
      bobSentFileNumber = fileNumber
      state.addTask { (tox, state) =>
        selfPublicKey = tox.getPublicKey
        debug(s"sending control RESUME for $fileNumber")
        debug(s"seek file to $bobOffset")
        tox.fileSeek(friendNumber, bobSentFileNumber, bobOffset)
        tox.fileControl(friendNumber, fileNumber, ToxFileControl.RESUME)
        state
      }
    }

    override def fileRecvChunk(friendNumber: Int, fileNumber: Int, position: Long, data: Array[Byte])(state: ChatState): ChatState = {
      assert(isBob)
      debug(s"receive file chunk from position $position of length ${data.length}")
      if (data.length == 0 && receivedData.length == bobOffset) {
        assert(receivedData sameElements fileData)
        debug("finish transmission")
        state.finish
      } else {
        System.arraycopy(data, 0, receivedData, position.toInt, data.length)
        bobOffset += data.length
        state
      }
    }

  }

}
