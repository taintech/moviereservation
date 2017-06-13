package com.taintech.moviereservation.actors

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.{PersistentActor, RecoveryCompleted}

class ReservationActor(movieTitleActor: ActorRef) extends PersistentActor with ActorLogging {
  import ReservationActor._

  override def persistenceId = "reservation-id"

  case class ReservationState(var movies: List[MovieInfo]) {
    def update: Event => Unit = {
      case MovieRegistered(imdbId, availableSeats, screenId) =>
        movies = MovieInfo(imdbId, screenId, None, availableSeats, 0) :: movies
      case SeatReserved(imdbId, screenId) =>
        for {
          (movie, id) <- movies.zipWithIndex
          if movie.imdbId == imdbId && movie.screenId == screenId
          updatedMovie = movie.copy(reservedSeats = movie.reservedSeats + 1)
        } movies = movies.updated(id, updatedMovie)
      case MovieTitleAssigned(imdbId, title) =>
        for {
          (movie, id) <- movies.zipWithIndex
          if movie.imdbId == imdbId
          updatedMovie = movie.copy(title = Some(title))
        } movies = movies.updated(id, updatedMovie)
    }
  }

  val state = ReservationState(Nil)

  override def receiveCommand: Receive = {
    case movieRegistered @ MovieRegistered(imdbId, _, screenId) =>
      if (state.movies.exists(e => e.imdbId == imdbId && e.screenId == screenId))
        sender() ! MovieAlreadyRegistered
      else
        persist(movieRegistered) { persistedEvent =>
          state.update(persistedEvent)
          sender() ! MovieRegisteredSuccessfully
          movieTitleActor ! imdbId
        }
    case seatReserved @ SeatReserved(imdbId, screenId) =>
      if (!state.movies.exists(e => e.imdbId == imdbId && e.screenId == screenId))
        sender() ! MovieNotFound
      else if (state.movies.exists(
                 e => e.imdbId == imdbId && e.screenId == screenId && e.reservedSeats >= e.availableSeats
               ))
        sender() ! NoSeatsAvailable
      else
        persist(seatReserved) { persistedEvent =>
          state.update(persistedEvent)
          sender() ! SeatReservedSuccessfully
        }
    case movieTitleAssigned: MovieTitleAssigned =>
      persist(movieTitleAssigned) { persistedEvent =>
        state.update(persistedEvent)
      }
    case GetMovieInfo(imdbId, screenId) =>
      state.movies.find(e => e.imdbId == imdbId && e.screenId == screenId) match {
        case Some(movieInfo @ MovieInfo(_, _, title, _, _)) =>
          if (title.isEmpty) { movieTitleActor ! imdbId }
          sender() ! movieInfo
        case None => sender() ! MovieNotFound
      }
  }

  override def receiveRecover: Receive = {
    case event: Event => state.update(event)
    case RecoveryCompleted => log.info("Recovery completed.")
  }
}

object ReservationActor {
  sealed trait Event
  final case class MovieRegistered(imdbId: String, availableSeats: Int, screenId: String) extends Event
  final case class SeatReserved(imdbId: String, screenId: String) extends Event
  final case class MovieTitleAssigned(imdbId: String, title: String) extends Event
  final case class MovieInfo(
    imdbId: String,
    screenId: String,
    title: Option[String],
    availableSeats: Int,
    reservedSeats: Int
  )
  final case class GetMovieInfo(imdbId: String, screenId: String)
  case object MovieRegisteredSuccessfully
  case object SeatReservedSuccessfully
  case object MovieNotFound
  case object MovieAlreadyRegistered
  case object NoSeatsAvailable
}
