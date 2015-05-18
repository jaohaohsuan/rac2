/**
 * Created by henry on 5/18/15.
 */
/**
 * Created by henry on 5/17/15.
 */
import akka.actor._
import akka.persistence._
import util._

import PermissionRepo.{ Event, Appended, SuccessAck }

object PathListing {

  case object GetPaths

  case class Sync(event: Event)

  def props = Props[PathListing]

  val persistenceId: String = "pathListing"

}

class PathListing extends PersistentActor with ImplicitActorLogging {

  import PathListing._

  override def persistenceId = PathListing.persistenceId

  var state = Set[String]()

  def updateState(event: Event): Unit = event match {
    case Appended(_, path, _, _) =>
      state += path
  }

  val receiveCommand: Receive = {
    case Sync(e) =>
      persist(e) { evt =>
        log.info(s"'${evt}' has been persisted @ ${persistenceId}")
        updateState(evt)
        sender() ! true
      }
    case GetPaths =>
      sender() ! state
    case other =>
      other.logInfo("unknown message received: " + _.toString)
  }

  val receiveRecover: Receive = {
    case e: Appended =>
      updateState(e)
    case SnapshotOffer(_, snapshot) =>

  }

}

