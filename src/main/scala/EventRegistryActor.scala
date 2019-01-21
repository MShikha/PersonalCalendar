import java.time.ZonedDateTime

import akka.actor.{Actor, ActorLogging, Props}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.ExecutionContext
import scala.util.Random

final case class PersonalCalendar(eventName: String, eventTime: ZonedDateTime, eventDuration: Float)

final case class PersonalCalendars(events: Seq[PersonalCalendar])

object EventRegistryActor {

  final case class ActionPerformed(description: String)

  final case class GetCalendar(eventName: String)

  final case class CreateCalendar(event: PersonalCalendar)

  final case class GetCalendarQuery(year: String, month: String, day: String, time: String)

  final case class GetCalendarQueryByEventName(eventName: String)

  val db1 = Database.forConfig("slick-postgres")

  class PCalendar(tag: Tag) extends Table[(Int, String, String, String)](tag, "PCALENDAR") {
    def id = column[Int]("EVENT_ID", O.PrimaryKey) // This is the primary key column
    def eventName = column[String]("EVENT_NAME")
    def date = column[String]("EVENT_DATE")
    def duration = column[String]("EVENT_DURATION")
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, eventName, date, duration)
  }

  val pCalendar = TableQuery[PCalendar]

  def props: Props = Props[EventRegistryActor]

  val rn = new Random()

}

class EventRegistryActor extends Actor with ActorLogging {

  import EventRegistryActor._

  var events = Set.empty[PersonalCalendar]
  implicit val executionContext: ExecutionContext = this.context.dispatcher

// Todo: Check if schema is already created
//  try {
//    def createTableIfNotInTables(tables: Vector[MTable]): Future[Unit] = {
//      if (!tables.exists(_.name.name == pCalendar.baseTableRow.tableName)) {
//        db1.run(pCalendar.schema.create)
//      } else {
//        Future()
//      }
//    }
//
//    val createTableIfNotExist: Future[Unit] = db1.run(MTable.getTables).flatMap(createTableIfNotInTables)
//
//    Await.result(createTableIfNotExist, Duration.Inf)
//  } finally db1.close



  def receive: Receive = {
    case GetCalendar =>

      // Todo: Get result from databse and parse it in immutable.Seq
//      var eventsDb = Set.empty[PersonalCalendar]
//      try
//        {
//          val q = for (c <- pCalendar) yield c
//          val a = q.result
//          val f: Future[Seq[(Int,String,String,String)]] = db1.run(a)
//
//          f.onSuccess { case s => println(s"Result: $s") }
//        }
//      finally  db1.close()
//
      sender() ! PersonalCalendars(events.toSeq)
    case GetCalendarQueryByEventName(evtName) =>
      val eventList = events.filter(_.eventName.contains(evtName))
      sender() ! PersonalCalendars(eventList.toSeq)
    case CreateCalendar(event) =>

      // Todo: insert row into table
//      try {
//        Await.result(db1.run(DBIO.seq(
//          pCalendar += (rn.nextInt(100) + 1,event.eventName,event.eventTime.toString,event.eventDuration.toString),
//          pCalendar.result.map(println))), Duration.Inf)
//      } finally db1.close

      events += event
      sender() ! ActionPerformed(s"Event  ${event.eventName} created.")
    case someQuery: GetCalendarQuery =>
      val day: Boolean = someQuery.day.isEmpty
      val month: Boolean = someQuery.month.isEmpty
      val year: Boolean = someQuery.year.isEmpty
      val time: Boolean = someQuery.time.isEmpty

      val eventCalendars =
        if (day && month && year && time) {
          throw new Exception("No Matching creteria found")
        }
        else {
          events.filter {
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

