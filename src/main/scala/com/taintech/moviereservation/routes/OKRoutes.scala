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

trait OKRoutes {

  def ok: Route = (get & path("OK")) {
    complete(StatusCodes.OK, "OK")
  }

}
