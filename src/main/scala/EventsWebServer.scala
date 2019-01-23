import EventRegistryActor._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}


object EventsWebServer extends App with EventsRoutes with JsonSupport {


  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("eventsAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  val db1 = Database.forConfig("slick-postgres")
  val eventRegistryActor: ActorRef = system.actorOf(EventRegistryActor.props(db1), "eventRegistryActor")

  lazy val routes: Route = eventRoutes
  implicit val timeout: Timeout = Timeout(50 seconds)


  try {
    def createTableIfNotInTables(tables: Vector[MTable]): Future[Unit] = {
      if (!tables.exists(_.name.name == pCalendar.baseTableRow.tableName)) {
        db1.run(pCalendar.schema.create)
      } else {
        Future()
      }
    }

    val createTableIfNotExist: Future[Unit] = db1.run(MTable.getTables).flatMap(createTableIfNotInTables)
    Await.result(createTableIfNotExist, Duration.Inf)
  }


  lazy val eventRoutes: Route =
    path("allEvents") {
      get {
        val eventsList: Future[PersonalCalendars] =
          (eventRegistryActor ? GetCalendar).mapTo[PersonalCalendars]
        complete(ToResponseMarshallable(eventsList))
      } ~
        post {
          entity(as[PersonalCalendar]) { evt =>
            val eventCreated: Future[ActionPerformed] =
              (eventRegistryActor ? CreateCalendar(evt)).mapTo[ActionPerformed]
            onSuccess(eventCreated) { performed =>
              complete(ToResponseMarshallable(StatusCodes.Created, performed))
            }
          }
        }
    } ~
      path("queryEvents") {
        get {
          parameter('year.as[String], 'month.as[String], 'day.as[String], 'time.as[String]) {
            (year, month, day, time) =>

              val eventsList: Future[PersonalCalendars] =
                (eventRegistryActor ? GetCalendarQuery(year, month, day, time)).mapTo[PersonalCalendars]
              onSuccess(eventsList) { abc => {
                complete(ToResponseMarshallable(abc))
              }
              }

          }
        }
      } ~
      path("queryEventsByName") {
        get {
          parameter('eventName.as[String]) {
            (eventName) =>
              val eventsList: Future[PersonalCalendars] =
                (eventRegistryActor ? GetCalendarQueryByEventName(eventName)).mapTo[PersonalCalendars]
              onSuccess(eventsList) { getEvent => {
                complete(ToResponseMarshallable(getEvent))
              }
              }

          }
        }
      }

  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 8080)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http:// ${bound.localAddress.getHostString}: ${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

system.registerOnTermination(db1.close())
//  try {
//    sys.addShutdownHook {
//      serverBinding.flatMap(_.unbind())
//      system.terminate()
//      db1.close()
//    }
//  }
//  catch {
//    case NonFatal(e) =>
//      println("Failed to start Web Server application.", e)
//      system.terminate()
//      db1.close()
//  }


}

trait EventsRoutes





