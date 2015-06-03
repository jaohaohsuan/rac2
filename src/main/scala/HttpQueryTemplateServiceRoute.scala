/**
 * Created by henry on 6/1/15.
 */

import org.apache.lucene.search.BooleanClause.Occur
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

case class Clause(title: String)

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

  val clauseRegex = """^match$|^near$|^named$""".r
  val occurRegex = """^must$|^must_not$|^should$""".r

  val queryTemplatePathMatcher = Segment / "template" / "_query" / Segments

  object _query extends PathMatcher1[String] {
    def apply(path: Path) = {

      val pathString = path.toString
      """^[^_]*""".r.findFirstIn(path.toString) match {
        case Some(s) => Matched(Path(pathString.substring(s.length)), s :: HNil)
        case None => Unmatched
      }
    }
  }

  def queryTemplateRoute = pathPrefix(_query ~ "_query" / "template" / Segment) { (realm, id) =>

    complete(id)


  }

  def queryRoute =

    respondWithMediaType(`application/vnd.collection+json`) {
      URI { href =>

        pathSuffix(queryTemplatePathMatcher) { (id, segs) =>

          val links = List(
            Link(href.resolve(s"${href.getPath}/must"), "section"),
            Link(href.resolve(s"${href.getPath}/must_not"), "section"),
            Link(href.resolve(s"${href.getPath}/should"), "section"),
            Link(href.resolve(s"${href.getPath}/clauses/match"), "edit"),
            Link(href.resolve(s"${href.getPath}/clauses/near"), "edit"),
            Link(href.resolve(s"${href.getPath}/clauses/named"), "edit"))

          complete(JsonCollection(href, links, List.empty))
        } ~
          pathSuffix(occurRegex / queryTemplatePathMatcher) { (occur: String, id: String, segments: List[String]) =>

            val sampleItem = Item(href.resolve(s"${href.getPath}/match/32341"), Clause("基金 沪指 风险"), List.empty)

            complete(JsonCollection(href, List.empty, List(sampleItem)))
          } ~
          pathSuffix(clauseRegex / "clauses" / queryTemplatePathMatcher) { (clause: String, id: String, segments: List[String]) =>

            get {
              val template: Option[Template] = clause match {
                case "match" => MatchClause("", "AND", "")
                case "near" => SpanNearClause("", 1, false, "")
                case "named" => NamedClause("", "")
              }
              complete(JsonCollection(href, List.empty, List.empty, List.empty, template))
            } ~
              post {
                handleWith(appendMatchClause(id)) ~ handleWith(appendSpanNearClause)
              }
          } ~
          pathSuffix(IntNumber / clauseRegex / occurRegex / queryTemplatePathMatcher) { (clauseId: Int, clause: String, occur: String, id: String, segments: List[String]) =>
            get {
              val clauseObj = clause match {
                case "match" => MatchClause("", "AND", "")
                case "near" => SpanNearClause("", 1, false, "")
                case "named" => NamedClause("", "")
              }
              complete(JsonCollection(href.resolve(""), List.empty, Item(href, clauseObj, List.empty)))
            } ~ post {
              complete("updated")
            } ~ delete {
              complete("deleted")
            }

          }
      }

    }
}
