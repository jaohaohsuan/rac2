/**
 * Created by henry on 5/17/15.
 */

import akka.actor._
import akka.contrib.pattern.ShardRegion
import akka.persistence._
import akka.pattern.{ ask }
import scala.util.{ Success, Failure }
import util._
import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._
import akka.util.Timeout

object PermissionRepo {

  sealed trait Command {
    def repoId: String
  }

  case class Append(user: String, path: String, operation: String, dueDate: Long) extends Command {
    def repoId = user.take(1)
  }

  sealed trait Query {
    def repoId: String
  }

  case class Get(user: String) extends Query {
    def repoId = user.take(1)
  }

  case class PathPermission(path: String, operation: String, dueDate: String)

  sealed trait Ack

  case class SuccessAck(message: String) extends Ack

  sealed trait Event

  case class Appended(user: String, path: String, operation: String, dueDate: Long) extends Event

  def props(userListing: ActorRef, pathListing: ActorRef) = Props(new PermissionRepo(userListing, pathListing))

  val shardName = "permissionRepo"

  val idExtractor: ShardRegion.IdExtractor = {
    case c: Command => (c.repoId, c)
    case q: Query => (q.repoId, q)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case c: Append => math.abs(c.repoId.hashCode) % 100 toString
    case q: Query => math.abs(q.repoId.hashCode) % 100 toString
  }

}

class PermissionRepo(userListingRegion: ActorRef, pathListing: ActorRef) extends PersistentActor with ImplicitActorLogging {

  import PermissionRepo._

  override def persistenceId = s"${self.path.parent.name}-${self.path.name}".logInfo(_.toString)

  var state: Map[String, Map[(String, String), Long]] = Map.empty

  def updateState(event: Event) = event match {
    case Appended(user, path, operation, dueDate) =>
      state += (user -> state.getOrElse(user, Map.empty).+((path, operation) -> dueDate))
  }

  val receiveCommand: Receive = {
    case Append(user, path, operation, dueDate) =>
      val originSender = sender()
      persist(Appended(user, path, operation, dueDate)) { evt =>
        log.info(s"'${evt}' has been persisted @ ${persistenceId}")
        updateState(evt)

        implicit val executionContext = context.dispatcher
        implicit val timeout = Timeout(30 seconds)

        (for {
          a <- (userListingRegion ? UserListing.Sync(evt)).mapTo[Boolean]
          b <- (pathListing ? PathListing.Sync(evt)).mapTo[Boolean]
        } yield a && b).onComplete {
          case Success(true) => originSender ! SuccessAck("OK")
          case Success(false) => log.error("sync fail")
          case Failure(e) => log.error(e.getMessage)
        }

      }
    case Get(user) =>
      sender() ! state.getOrElse(user, Map.empty).map {
        case ((path, operation), dueDate) => PathPermission(path, operation, dueDate.toString)
      }.toList
  }

  val receiveRecover: Receive = {
    case e: Appended =>
      updateState(e)
    case SnapshotOffer(_, snapshot) =>

  }

}
