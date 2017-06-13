package com.taintech.moviereservation

import akka.actor.{ ActorSystem, Props }
import akka.event.{ Logging, LoggingAdapter }
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.taintech.moviereservation.actors.{ MovieTitleActor, ReservationActor }
import com.taintech.moviereservation.routes.ReservationService
import com.taintech.moviereservation.utils.Config

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App with Config {

  implicit val system = ActorSystem("moviereservation")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val log: LoggingAdapter = Logging(system, getClass)
  implicit val timeout: Timeout = 2.seconds
  val imdbUrlRoot = "http://www.imdb.com/title"
  val http = Http()
  val movieTitleActor = system.actorOf(Props(new MovieTitleActor(http, imdbUrlRoot)), "title")
  val reservationActor = system.actorOf(Props(new ReservationActor(movieTitleActor)), "reservation")
  val reservationService = new ReservationService(reservationActor)

  val bindingFuture = http.bindAndHandle(reservationService.route, httpHost, httpPort)
  log.info(s"Server online at http://$httpHost:$httpPort/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture.flatMap(_.unbind()).onComplete(_ ⇒ system.terminate())

}
