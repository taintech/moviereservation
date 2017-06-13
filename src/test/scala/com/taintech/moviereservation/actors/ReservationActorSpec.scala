package com.taintech.moviereservation.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ReservationActorSpec
    extends TestKit(
      ActorSystem(
        "ReservationActorSpec",
        ConfigFactory.parseString("""akka.persistence {
                                    |  journal.plugin = "inmemory-journal"
                                    |}""".stripMargin)
      )
    )
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  import ReservationActor._
  import ReservationActorSpec._

  override def afterAll {
    TestKit.shutdownActorSystem(system)

  }

  "A reservation actor" must {
    val probe = TestProbe()
    val reservationActor = system.actorOf(Props(new ReservationActor(probe.ref)))
    "register a movie" in {
      reservationActor ! MovieRegistered(testImdbId, 3, testScreenId)
      expectMsg(MovieRegisteredSuccessfully)
      probe.expectMsg(testImdbId)
      probe.reply(MovieTitleAssigned(testImdbId, testMovieTitle))
    }
    "reserve a seat at the movie" in {
      reservationActor ! SeatReserved(testImdbId, testScreenId)
      expectMsg(SeatReservedSuccessfully)
    }
    "retrieve information about the movie" in {
      reservationActor ! GetMovieInfo(testImdbId, testScreenId)
      expectMsg(MovieInfo(testImdbId, testScreenId, Some(testMovieTitle), 3, 1))
    }
    "not register an existing movie" in {
      reservationActor ! MovieRegistered(testImdbId, 100, testScreenId)
      expectMsg(MovieAlreadyRegistered)
    }
    "not reserve more than available seats" in {
      reservationActor ! SeatReserved(testImdbId, testScreenId)
      expectMsg(SeatReservedSuccessfully)
      reservationActor ! SeatReserved(testImdbId, testScreenId)
      expectMsg(SeatReservedSuccessfully)
      reservationActor ! SeatReserved(testImdbId, testScreenId)
      expectMsg(NoSeatsAvailable)
    }
    "not reserve seat for an unknown movie" in {
      reservationActor ! SeatReserved("unknownMovie",  testScreenId)
      expectMsg(MovieNotFound)
    }
    "not reserve seat for an unknown screenId" in {
      reservationActor ! SeatReserved(testImdbId, "unknownScreen")
      expectMsg(MovieNotFound)
    }
    "retrieve information about the movie with correct reserved seats" in {
      reservationActor ! GetMovieInfo(testImdbId, testScreenId)
      expectMsg(MovieInfo(testImdbId, testScreenId, Some(testMovieTitle), 3, 3))
    }
    "not retrieve information for an unknown movie" in {
      reservationActor ! GetMovieInfo("unknownMovie",  testScreenId)
      expectMsg(MovieNotFound)
    }
    "not retrieve information for an unknown screenId" in {
      reservationActor ! GetMovieInfo(testImdbId, "unknownScreen")
      expectMsg(MovieNotFound)
    }
  }
}

object ReservationActorSpec {
  val testImdbId = "testImdbId"
  val testScreenId = "testScreenId"
  val testMovieTitle = "testMovieTitle"
}
