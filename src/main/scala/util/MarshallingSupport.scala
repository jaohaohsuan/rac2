package util

/**
 * Created by henry on 5/17/15.
 */
import org.json4s.{ DefaultFormats, Formats }

import spray.httpx.Json4sSupport

object MarshallingSupport extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats
}

object CollectionJsonSupport {

  import MarshallingSupport._
  import net.hamnaberg.json.collection._
  import net.hamnaberg.json.collection.data._
  import spray.httpx.marshalling._
  import spray.httpx.unmarshalling._

  import org.json4s.native.JsonMethods._

  import spray.http.{ MediaType, MediaTypes, HttpEntity }

  val `application/vnd.collection+json` = MediaTypes.register(MediaType.custom("application/vnd.collection+json"))

  implicit val templateUnmarshaller: Unmarshaller[Template] =
    Unmarshaller[Template](`application/vnd.collection+json`) {
      case HttpEntity.NonEmpty(contentType, data) =>
        val string = data.asString
        NativeJsonCollectionParser.parseTemplate(string) match {
          case Right(o: Template) => o
          case Left(e) =>
            throw e
        }
    }

  implicit def templateToObjectUnmarshaller[T <: AnyRef: Manifest]: Unmarshaller[T] =
    Unmarshaller.delegate[Template, T](`application/vnd.collection+json`) { template =>
      new JavaReflectionData[T].unapply(template.data) match {
        case Some(o) => o
        case None =>
          throw new Exception(s"Unable to convert Template to '$manifest.getClass.getName' class.")
      }
    }

  implicit val collectionJsonMarshaller: Marshaller[JsonCollection] =
    Marshaller.of[JsonCollection](`application/vnd.collection+json`) { (value, contentType, ctx) =>
      ctx.marshalTo(HttpEntity(contentType, compact(render(value.toJson))))
    }

  import scala.language.implicitConversions

  implicit def asTemplate[T <: AnyRef: Manifest](value: T)(implicit formats: org.json4s.Formats): Option[Template] =
    Some(Template(value)(dataApply(manifest, formats)))

  implicit def dataApply[T <: AnyRef: Manifest](implicit formats: org.json4s.Formats): DataApply[T] = {
    new JavaReflectionData[T]()(formats, manifest[T])
  }
}
