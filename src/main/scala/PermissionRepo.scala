/**
 * Created by henry on 5/17/15.
 */

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence._
import util._

object PermissionRepo {

  sealed trait Command {
    def repoId: String
  }

  case class Append(user: String, path: String, operation: String, dueDate: Long) extends Command {
    def repoId = user.take(1)
  }

  sealed trait Ack

  case class SuccessAck(repoId: String) extends Ack

  sealed trait Event

  case class Appended(user: String, path: String, operation: String, dueDate: Long) extends Event

  def props(userListing: ActorRef) = Props(new PermissionRepo(userListing))

  val shardName = "permissionRepo"

  val idExtractor: ShardRegion.IdExtractor = {
    case c: Command => (c.repoId, c)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case c: Append => math.abs(c.repoId.hashCode) % 100 toString
  }

}

class PermissionRepo(userListingRegion: ActorRef) extends PersistentActor with ImplicitActorLogging {

  import PermissionRepo._

  override def persistenceId = s"${self.path.parent.name}-${self.path.name}".logInfo(_.toString)

  case class ProcessedCommand(event: Option[Event], ack: Ack, newReceive: Option[Receive])

  def handleProcessedCommand(originSender: ActorRef, event: Option[Event], ack: Ack) = {
    event.fold(sender() ! ack) {
      persist(_) { evt =>
        updateState(evt)
        originSender ! ack
      }
    }
  }

  var state: Map[String, Map[(String, String), Long]] = Map.empty

  def updateState(event: Event) = event match {
    case Appended(user, path, operation, dueDate) =>
      userListingRegion ! event
      state += (user -> state.getOrElse(user, Map.empty).+((path, operation) -> dueDate))
  }

  val receiveCommand: Receive = {
    case c @ Append(user, path, operation, dueDate) =>
      c.logInfo(_.toString)
      handleProcessedCommand(sender(), Some(Appended(user, path, operation, dueDate)), SuccessAck(c.repoId))
  }

  val receiveRecover: Receive = {
    case e: Appended =>
      updateState(e)
    case SnapshotOffer(_, snapshot) =>

  }

}