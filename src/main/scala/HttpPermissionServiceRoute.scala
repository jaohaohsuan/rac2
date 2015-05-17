/**
 * Created by henry on 5/17/15.
 */
import spray.routing._
import spray.http.StatusCodes._
import akka.actor.{ ActorRef }
import akka.pattern.{ ask }

import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.{ Try, Success, Failure }

import shapeless.HNil

final case class Register(user: String, httpVerb: String, dueDate: String) {
  require(!user.isEmpty, "user must not be empty")
}

trait HttpPermissionServiceRoute extends HttpService {

  import util.MarshallingSupport._

  import PermissionRepo._

  implicit val executionContext: ExecutionContext
  implicit val timeout = Timeout(30 seconds)

  val useAccessorFirstCharAsRepoId: Directive1[(String, String)] =
    parameters('user.as[String]).flatMap {
      case "" =>
        reject
      case x =>
        provide((x.take(1), x))
    }

  val InvalidDateTimeFormatHandler = ExceptionHandler {
    case e: IllegalArgumentException =>
      complete(BadRequest, e.toString)
  }

  def route(repoRegion: ActorRef) = {
    post {
      pathPrefix("permission" / Segments) { segments ⇒
        entity(as[Register]) { o =>
          handleExceptions(InvalidDateTimeFormatHandler) {
            onComplete({

              import com.github.nscala_time.time.Imports._
              repoRegion ? Append(
                o.user,
                segments.mkString("/"),
                o.httpVerb,
                DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseMillis(o.dueDate))

            }.mapTo[Ack]) {
              case Success(ack) ⇒ ack match {
                case SuccessAck(repoId) ⇒
                  complete(repoId)
              }
              case Failure(e) ⇒
                complete(e.getMessage)
            }
          }
        }

      }
    } ~
      get {
        pathPrefix("permission") {
          //          pathEnd {
          //            useAccessorFirstCharAsRepoId {
          //              case (repoId, user) => {
          //                onComplete((accesserQuery ? QueryIndividualPermissions(repoId, accesser)).mapTo[IndividualPermissionsResponse]) {
          //                  case Success(s) ⇒
          //                    complete(s)
          //                  case Failure(e) ⇒
          //                    complete(e.toString)
          //                }
          //
          //              }
          //            }
          //          } ~
          path(Segments) { segments =>
            complete(segments)
          }
        }
      }

  }

}

