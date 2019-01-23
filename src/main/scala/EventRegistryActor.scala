import java.time.ZonedDateTime

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.ActorMaterializer
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Random

final case class PersonalCalendar(eventName: String, eventTime: ZonedDateTime, eventDuration: Float)

final case class PersonalCalendars(events: Seq[PersonalCalendar])

object EventRegistryActor {

  final case class ActionPerformed(description: String)

  final case class GetCalendar(eventName: String)

  final case class CreateCalendar(event: PersonalCalendar)

  final case class GetCalendarQuery(year: String, month: String, day: String, time: String)

  final case class GetCalendarQueryByEventName(eventName: String)


  class PCalendar(tag: Tag) extends Table[(String, String, String)](tag, "PCALENDAR") {
    def eventName = column[String]("EVENT_NAME")

    def date = column[String]("EVENT_DATE")

    def duration = column[String]("EVENT_DURATION")

    // Every table needs a * projection with the same type as the table's type parameter
    def * = (eventName, date, duration)
  }

  val pCalendar = TableQuery[PCalendar]


  def props(db1: Database): Props = Props(new EventRegistryActor(db1))

  val rn = new Random()

}

class EventRegistryActor(db1: Database) extends Actor with ActorLogging {

  import EventRegistryActor._

  var events = Set.empty[PersonalCalendar]
  var eventsDb = Set.empty[PersonalCalendar]
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = this.context.dispatcher

    def receive: Receive = {
    case GetCalendar =>

       try {
        val q = for (c <- pCalendar) yield c
        db1.run(q.result.map(_.foreach {
          case someVal: Any =>
            eventsDb += PersonalCalendar(someVal._1, ZonedDateTime.parse(someVal._2), someVal._3.toFloat)
        }))
      }
      catch {
        case exception: Exception => throw new Exception(exception)
      }
      sender() ! PersonalCalendars(eventsDb.toSeq)


    case GetCalendarQueryByEventName(evtName) =>
      try {
        val q = for (c <- pCalendar) yield c
        db1.run(q.result.map(_.foreach {
          case someVal: Any =>
            eventsDb += PersonalCalendar(someVal._1, ZonedDateTime.parse(someVal._2), someVal._3.toFloat)
        }))
      }
      catch {
        case exception: Exception => throw new Exception(exception)
      }
      sender() ! PersonalCalendars(eventsDb.filter(_.eventName.contains(evtName)).toSeq)
    case CreateCalendar(event) =>
      try {
        Await.result(db1.run(DBIO.seq(
          pCalendar += (event.eventName, event.eventTime.toString, event.eventDuration.toString),
          pCalendar.result.map(println))), Duration.Inf)
      }
      catch {
        case exception: Exception => throw new Exception(exception)
      }
      eventsDb += event
      sender() ! ActionPerformed(s"Event  ${event.eventName} created.")


    case someQuery: GetCalendarQuery =>
      val day: Boolean = someQuery.day.isEmpty
      val month: Boolean = someQuery.month.isEmpty
      val year: Boolean = someQuery.year.isEmpty
      val time: Boolean = someQuery.time.isEmpty

      val eventCalendars =
        if (day && month && year && time) {
          throw new Exception("No Matching criteria found")
        }
        else {
          try {
            val q = for (c <- pCalendar) yield c
            db1.run(q.result.map(_.foreach {
              case someVal: Any =>
                eventsDb += PersonalCalendar(someVal._1, ZonedDateTime.parse(someVal._2), someVal._3.toFloat)
            }))
          }
          catch {
            case exception: Exception => throw new Exception(exception)
          }

          eventsDb.filter {
            cal =>
              (!year && (cal.eventTime.getYear.toString.contains(someQuery.year))) ||
                (!month && (cal.eventTime.getMonth.toString.contains(someQuery.month))) ||
                (!day && (cal.eventTime.getDayOfMonth.toString.contains(someQuery.day))) ||
                (!time && (cal.eventTime.getHour.toString.contains(someQuery.time)))
          }.toSeq
        }
      sender() ! PersonalCalendars(eventCalendars)
  }
}

