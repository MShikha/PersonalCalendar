import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import EventRegistryActor.ActionPerformed
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}


trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  val dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  implicit object DateJsonFormat extends RootJsonFormat[ZonedDateTime] {
  override def write(obj: ZonedDateTime) = JsString(dateTimeFormatter.format(obj))
   override def read(json: JsValue) = json match {
    case JsString(s) => ZonedDateTime.parse(s, dateTimeFormatter)
     case other => throw new DeserializationException("Cannot parse json value " + other + " as timestamp")
      }
    }

  implicit val eventJsonFormat = jsonFormat3(PersonalCalendar)
  implicit val eventsJsonFormat = jsonFormat1(PersonalCalendars)


  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
