package util

/**
 * Created by henry on 5/17/15.
 */
import org.json4s.{ DefaultFormats, Formats }
import spray.httpx.Json4sSupport

object MarshallingSupport extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats
}
