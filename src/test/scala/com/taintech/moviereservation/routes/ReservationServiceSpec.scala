package com.taintech.moviereservation.routes

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, MediaTypes, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.{ RouteTestTimeout, ScalatestRouteTest }
import akka.testkit.TestProbe
import akka.util.Timeout
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.duration._

class ReservationServiceSpec extends WordSpec with Matchers with ScalatestRouteTest {
  import ReservationServiceSpec._
  import com.taintech.moviereservation.actors.ReservationActor._

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(new DurationInt(5).second)
  implicit val log: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = 3.seconds

  "Reservation service" must {
    val probe = TestProbe()
    val route: Route = new ReservationService(probe.ref).route
    "register a movie with json request" in {
      val result = Post("/register-movie", RegisterMovieRequest) ~> route ~> runRoute
      probe.expectMsg(MovieRegistered("tt0111161", 100, "screen_123456"))
      probe.reply(MovieRegisteredSuccessfully)
      check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Movie registered."
      }(result)
    }
    "reserve a seat with json request" in {
      val result = Post("/reserve-seat", ReserveSeatRequest) ~> route ~> runRoute
      probe.expectMsg(SeatReserved("tt0111161", "screen_123456"))
      probe.reply(SeatReservedSuccessfully)
      check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Seat reserved."
      }(result)
    }
    "retrieve information about the movie as json response" in {
      val result = Get("/movie-info/tt0111161/screen_123456") ~> route ~> runRoute
      probe.expectMsg(GetMovieInfo("tt0111161", "screen_123456"))
      probe.reply(MovieInfo("tt0111161", "screen_123456", Some("The Shawshank Redemption"), 100, 50))
      check {
        status shouldEqual StatusCodes.OK
        responseEntity shouldEqual HttpEntity(
          ContentTypes.`application/json`,
          """{"availableSeats":100,"imdbId":"tt0111161","reservedSeats":50,"screenId":"screen_123456","title":"The Shawshank Redemption"}"""
        )
      }(result)
    }
  }
}

object ReservationServiceSpec {
  val RegisterMovieRequest = HttpEntity(
    MediaTypes.`application/json`,
    s"""{
       |  "imdbId": "tt0111161",
       |  "availableSeats": 100,
       |  "screenId": "screen_123456"
       |}
     """.stripMargin
  )
  val ReserveSeatRequest = HttpEntity(
    MediaTypes.`application/json`,
    s"""{
       |  "imdbId": "tt0111161",
       |  "screenId": "screen_123456"
       |}
     """.stripMargin
  )
}
