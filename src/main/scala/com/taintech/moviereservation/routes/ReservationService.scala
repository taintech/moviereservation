package com.taintech.moviereservation.routes

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.taintech.moviereservation.actors.ReservationActor._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class ReservationService(
  reservationActor: ActorRef
)(
  implicit val log: LoggingAdapter,
  implicit val timeout: Timeout,
  implicit val executionContext: ExecutionContextExecutor
) {

  implicit val reqisterMovieFormat = jsonFormat3(MovieRegistered)
  implicit val reserverSeatFormat = jsonFormat2(SeatReserved)
  implicit val movieInfoFormat = jsonFormat5(MovieInfo)

  val route: Route =
    post {
      path("register-movie") {
        entity(as[MovieRegistered]) { movieRegistered =>
          val result = (reservationActor ? movieRegistered) map {
            case MovieRegisteredSuccessfully => (StatusCodes.OK, "Movie registered.")
            case MovieAlreadyRegistered => (StatusCodes.BadRequest, "Bad request. Movie already registered.")
            case e =>
              log.error(s"Unexpected behaviour while registering $movieRegistered:\n" + e)
              (StatusCodes.InternalServerError, "Unexpected error while registering a movie.")
          }
          complete(result)
        }
      }
    } ~
      post {
        path("reserve-seat") {
          entity(as[SeatReserved]) { seatReserved =>
            val result = (reservationActor ? seatReserved) map {
              case SeatReservedSuccessfully => (StatusCodes.OK, "Seat reserved.")
              case NoSeatsAvailable => (StatusCodes.BadRequest, "No seats available.")
              case MovieNotFound => (StatusCodes.NotFound, "Movie not found.")
              case e =>
                log.error(s"Unexpected behaviour while reserving $seatReserved:\n" + e)
                (StatusCodes.InternalServerError, "Unexpected error while registering a movie.")
            }
            complete(result)
          }
        }
      } ~
      get {
        pathPrefix("movie-info" / Segment / Segment) {
          case (imdbId, screenId) =>
            onComplete(reservationActor ? GetMovieInfo(imdbId, screenId)) {
              case Success(movieInfo: MovieInfo) => complete(movieInfo)
              case Success(MovieNotFound) => complete(StatusCodes.NotFound, "Movie not found.")
              case Success(any) =>
                log.error(s"Unexpected behaviour while searching $GetMovieInfo:\n" + any)
                complete(StatusCodes.InternalServerError, "Unexpected error while searching for a movie.")
              case Failure(e) =>
                log.error(e, s"Failure while searching $GetMovieInfo:\n")
                complete(StatusCodes.InternalServerError, "Internal failure.")
            }
        }
      }
}
