/**
 * Created by henry on 6/1/15.
 */

import spray.routing.PathMatcher.{ Unmatched, Matched, Matching }
import spray.routing._
import spray.http.StatusCodes._
import akka.actor.{ ActorRef }
import akka.pattern.{ ask }
import spray.http.Uri.Path

import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.{ Try, Success, Failure }
import net.hamnaberg.json.collection._
import shapeless._

case class MatchClause(query: String, operator: String, occur: String)

case class SpanNearClause(query: String, slop: Int, inOrder: Boolean, occur: String)

case class NamedClause(uri: String, occur: String)

case class ClauseTitle(title: String)

trait HttpQueryTemplateServiceRoute extends HttpService {

  import util.MarshallingSupport._

  import util.CollectionJsonSupport._

  val URI = extract(ctx => java.net.URI.create(ctx.request.uri.toString))

  def appendMatchClause(templateId: String)(matchClause: MatchClause) = {
    "matchClause" + templateId
  }

  val appendSpanNearClause = (spanNearClause: SpanNearClause) => {
    "spanNearClause"
  }

  val clauseTypeRegex = """^match$|^near$|^named$""".r
  val occurRegex = """^must$|^must_not$|^should$""".r

  val clauseItemPathMatcher = clauseTypeRegex / IntNumber

  object Underline extends PathMatcher1[String] {
    def apply(path: Path) =
      """^[^_]*""".r.findPrefixMatchOf(path.toString) match {
        case Some(m) => Matched(Path(m.after.toString), m.matched :: HNil)
        case None => Unmatched
      }
  }

  def queryTemplateRoute = pathPrefix(Underline ~ "_query" / "template" / Segment) { (realm, id) =>
    respondWithMediaType(`application/vnd.collection+json`) {
      URI { href =>
        get {
          pathEndOrSingleSlash {
            val links = List(
              Link(href.resolve(s"${href.getPath}/must"), "section"),
              Link(href.resolve(s"${href.getPath}/must_not"), "section"),
              Link(href.resolve(s"${href.getPath}/should"), "section"),
              Link(href.resolve(s"${href.getPath}/match"), "edit"),
              Link(href.resolve(s"${href.getPath}/near"), "edit"),
              Link(href.resolve(s"${href.getPath}/named"), "edit"))

            complete(JsonCollection(href, links, List.empty))
          } ~
            path(occurRegex) { occur =>
              //query clauses of an occur
              val sampleItems = List(Item(href.resolve(s"${href.getPath.dropRight(occur.length)}match/32341"), ClauseTitle("基金 沪指 风险"), List.empty))
              complete(JsonCollection(href, List.empty, sampleItems))
            } ~
            path(clauseTypeRegex) { clauseType =>
              //shows template data of each clause type
              val template: Option[Template] = clauseType match {
                case "match" => MatchClause("", "AND", "")
                case "near" => SpanNearClause("", 1, false, "")
                case "named" => NamedClause("", "")
              }
              complete(JsonCollection(href, List.empty, List.empty, List.empty, template))
            } ~
            path(clauseItemPathMatcher) { (clauseType, clauseId) =>
              //details of a clause
              val clauseObj = clauseType match {
                case "match" => MatchClause("", "AND", "")
                case "near" => SpanNearClause("", 1, false, "")
                case "named" => NamedClause("", "")
              }
              complete(JsonCollection(href.resolve(""), List.empty, Item(href, clauseObj, List.empty)))
            }
        } ~
          post {
            path(clauseTypeRegex) { clauseType =>
              //add a new clause
              handleWith(appendMatchClause(id)) ~ handleWith(appendSpanNearClause)
            }
          } ~
          put {
            path(clauseItemPathMatcher) { (clauseType, clauseId) =>
              //update a clause
              handleWith(appendMatchClause(id)) ~ handleWith(appendSpanNearClause)
            }
          } ~
          delete {
            path(clauseItemPathMatcher) { (clauseType, clauseId) =>
              //delete a clause
              handleWith(appendMatchClause(id)) ~ handleWith(appendSpanNearClause)
            } ~
              path(occurRegex) { occur =>
                //remove all clause
                complete(OK)
              }
          }
      }
    }
  }
}
