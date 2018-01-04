package com.taintech.moviereservation.routes

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.taintech.moviereservation.actors.ReservationActor._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success }

class ReservationService(
  reservationActor: ActorRef
)(
  implicit val log: LoggingAdapter,
  implicit val timeout: Timeout,
  implicit val executionContext: ExecutionContextExecutor
) extends OKRoutes {

  implicit val reqisterMovieFormat = jsonFormat3(MovieRegistered)
  implicit val reserverSeatFormat = jsonFormat2(SeatReserved)
  implicit val movieInfoFormat = jsonFormat5(MovieInfo)

  val route: Route =
    ok ~ post {
      path("register-movie") {
        entity(as[MovieRegistered]) {
          onReservationCommandComplete {
            case MovieRegisteredSuccessfully => (StatusCodes.OK, "Movie registered.")
            case MovieAlreadyRegistered      => (StatusCodes.BadRequest, "Bad request. Movie already registered.")
          }
        }
      }
    } ~
      post {
        path("reserve-seat") {
          entity(as[SeatReserved]) {
            onReservationCommandComplete {
              case SeatReservedSuccessfully => (StatusCodes.OK, "Seat reserved.")
              case NoSeatsAvailable         => (StatusCodes.BadRequest, "No seats available.")
            }
          }
        }
      } ~
      get {
        pathPrefix("movie-info" / Segment / Segment) {
          case (imdbId, screenId) =>
            onReservationCommandComplete {
              case movieInfo: MovieInfo => movieInfo
            }(GetMovieInfo(imdbId, screenId))
        }
      }

  def onReservationCommandComplete[T](pf: PartialFunction[Any, ToResponseMarshallable]): T => Route =
    cmd =>
      onComplete(reservationActor ? cmd) {
        case Success(result) if pf.isDefinedAt(result) => complete(pf(result))
        case Success(MovieNotFound)                    => complete(StatusCodes.NotFound, "Movie not found.")
        case Success(otherResult) =>
          log.error(s"Unexpected result while performing command $cmd:\n" + otherResult)
          complete(StatusCodes.InternalServerError, "Unexpected error.")
        case Failure(e) =>
          log.error(e, s"Failure while performing command $cmd:\n")
          complete(StatusCodes.InternalServerError, "Internal failure.")
    }
}
