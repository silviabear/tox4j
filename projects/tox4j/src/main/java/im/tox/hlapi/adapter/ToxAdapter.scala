package im.tox.hlapi.adapter

import im.tox.hlapi.adapter.parser.{ ClientNotifier, UiEventParser, RequestParser, NetworkEventParser }
import im.tox.hlapi.adapter.performer.DiskIOActionPerformer
import im.tox.hlapi.event.{ UiEvent, NetworkEvent }
import im.tox.hlapi.listener.ToxClientListener
import im.tox.hlapi.performer.NetworkActionPerformer
import im.tox.hlapi.request.{ Reply, Request }
import im.tox.hlapi.state.ConnectionState.ConnectionOptions
import im.tox.hlapi.state.{ FriendState, CoreState }
import im.tox.hlapi.state.CoreState.ToxState
import im.tox.tox4j.core.options.ToxOptions
import im.tox.tox4j.impl.jni.ToxCoreImpl

import RequestParser._

import scala.collection.immutable.Queue

@SuppressWarnings(Array("org.brianmckenna.wartremover.warts.Null"))
final class ToxAdapter(toxClientListener: ToxClientListener) {

  private var state: ToxState = null
  private var tox: ToxCoreImpl[Queue[NetworkEvent]] = null

  def acceptUiEvent(e: UiEvent): Unit = {
    if (state == null) {
      println("Please init a new tox session first.")
    }
    val decision = UiEventParser.parse(state, e)
    state = {
      decision._2 match {
        case Some(action) => NetworkActionPerformer.perform(action, tox, decision._1)
        case None         => decision._1
      }
    }
  }

  def initToxSession(connectionOptions: ConnectionOptions): Unit = {
    val result = NetworkActionPerformer.initConnection(connectionOptions)
    tox = result._1
    state = result._2
  }

  def closeToxSession(): Unit = {
    tox.close()
    tox = null
    state = null
  }

  def acceptNetworkEvent(e: NetworkEvent): Unit = {
    val decision = NetworkEventParser.parse(state, e)
    state = {
      decision._2 match {
        case Some(action) => DiskIOActionPerformer.perform(decision._1)
        case None         => decision._1
      }
    }
    ClientNotifier.notify(toxClientListener, e)
  }

  def iterate(queue: Queue[NetworkEvent]): Queue[NetworkEvent] = {
    tox.iterate(queue)
  }

  def getIterateInterval: Int = {
    tox.iterationInterval
  }

  def isInit: Boolean = tox != null

  def acceptRequest(request: Request): Reply = {
    parseRequest(request, state)
  }

}

