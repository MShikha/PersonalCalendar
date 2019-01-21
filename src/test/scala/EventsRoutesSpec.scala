import EventRegistryActor.GetCalendar
import akka.actor.ActorRef
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.pattern.ask
import akka.testkit.TestKit
import akka.util.Timeout
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class EventsRoutesSpecRoutesSpec extends WordSpec with Matchers  with ScalatestRouteTest
  with EventsRoutes with JsonSupport {

  val eventRegistryActor: ActorRef =
    system.actorOf(EventRegistryActor.props, "eventsRegistry")
  implicit val timeout: Timeout = Timeout(50 seconds)


  val eventRoutes = path("allEvents") {
    get {
      val eventsList: Future[PersonalCalendars] =
        (eventRegistryActor ? GetCalendar).mapTo[PersonalCalendars]
      complete(ToResponseMarshallable(eventsList))
    }
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val routes = eventRoutes
  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 8080)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http:// ${bound.localAddress.getHostString}: ${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  "EventRoutes" should {
    "return all registered events  (GET /allEvents)" in {

      val request = HttpRequest(uri = "/allEvents")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        contentType should ===(ContentTypes.`application/json`)
        entityAs[String] should ===("""{"events":[]}""")
      }
    }

    // TODO: more unit tests should be added to test code functionality.

  }

}