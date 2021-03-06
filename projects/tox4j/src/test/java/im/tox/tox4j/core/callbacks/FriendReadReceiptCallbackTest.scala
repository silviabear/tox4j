package im.tox.tox4j.core.callbacks

import im.tox.tox4j.TestConstants.ITERATIONS
import im.tox.tox4j.core.enums.{ ToxConnection, ToxMessageType }
import im.tox.tox4j.testing.autotest.{ AliceBobTest, AliceBobTestBase }

final class FriendReadReceiptCallbackTest extends AliceBobTest {

  protected override def ignoreTimeout = true

  override type State = Set[Int]
  override def initialState: State = Set.empty[Int]

  protected override def newChatClient(name: String, expectedFriendName: String) = new ChatClient(name, expectedFriendName) {

    override def friendConnectionStatus(friendNumber: Int, connectionStatus: ToxConnection)(state: ChatState): ChatState = {
      super.friendConnectionStatus(friendNumber, connectionStatus)(state)
      if (connectionStatus != ToxConnection.NONE) {
        state.addTask { (tox, state) =>
          debug(s"Sending $ITERATIONS messages")
          assert(state.get.isEmpty)
          val pendingIds = (0 until ITERATIONS).foldLeft(state.get) {
            case (receipts, i) =>
              val receipt = tox.friendSendMessage(friendNumber, ToxMessageType.NORMAL, 0, String.valueOf(i).getBytes)
              assert(!receipts.contains(receipt))
              receipts + receipt
          }
          assert(pendingIds.size == ITERATIONS)
          state.set(pendingIds)
        }
      } else {
        state
      }
    }

    override def friendReadReceipt(friendNumber: Int, messageId: Int)(state: ChatState): ChatState = {
      assert(friendNumber == AliceBobTestBase.FRIEND_NUMBER)

      assert(state.get.contains(messageId))
      val pendingIds = state.get - messageId
      if (pendingIds.isEmpty) {
        state.finish
      } else {
        state.set(pendingIds)
      }
    }

  }

}
