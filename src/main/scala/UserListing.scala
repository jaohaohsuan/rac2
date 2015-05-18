/**
 * Created by henry on 5/17/15.
 */

import akka.contrib.pattern.ShardRegion
import akka.actor._
import akka.persistence._
import util._

import PermissionRepo._

object UserListing {

  case class GetUsers(path: String)

  case class Sync(originSender: ActorRef, event: Event)

  def props = Props[UserListing]

  val shardName = "userListing"

  val idExtractor: ShardRegion.IdExtractor = {
    case m @ GetUsers(path) => (path.take(1), m)
    case m @ Sync(_, e: Appended) => (e.path.take(1), m)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case GetUsers(path) => math.abs(path.take(1).hashCode) % 100 toString
    case m @ Sync(_, e: Appended) => math.abs(e.path.take(1).hashCode) % 100 toString
  }

}

class UserListing extends PersistentActor with ImplicitActorLogging {

  import UserListing._

  override def persistenceId = s"${self.path.parent.name}-${self.path.name}".logInfo(_.toString)

  var state: Map[String, Map[String, (String, Long)]] = Map.empty

  def updateState(event: Event): Unit = event match {
    case Appended(user, path, operation, dueDate) =>
      state += (path -> state.getOrElse(user, Map.empty).+(user -> (operation, dueDate)))
  }

  val receiveCommand: Receive = {
    case Sync(originSender, e) =>
      persist(e) { evt =>
        log.info(s"'${evt}' has been persisted @ ${persistenceId}")
        updateState(evt)
        originSender ! SuccessAck("OK")
      }
    case GetUsers(path) =>
      sender() ! state.getOrElse(path, Map.empty)
    case other =>
      other.logInfo("unknown message received: " + _.toString)
  }

  val receiveRecover: Receive = {
    case e: Appended =>
      updateState(e)
    case SnapshotOffer(_, snapshot) =>

  }

}
