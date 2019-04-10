package com.taintech.moviereservation.actors

import akka.actor.{ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{HttpExt, HttpsConnectionContext}
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{DefaultTimeout, EventFilter, ImplicitSender, TestKit}
import com.taintech.moviereservation.actors.ReservationActor.MovieTitleAssigned
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContextExecutor, Future}

class MovieTitleActorSpec
    extends TestKit(
      ActorSystem(
        "MovieTitleActorSpec",
        ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")
      )
    )
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with MockitoSugar
    with DefaultTimeout {
  import MovieTitleActorSpec._

  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A movie title actor" must {
    "send http request and reply with movie title" in {
      val httpMock = httpMockWithTitle(imdbTestId, imdbTestTitle)
      val titleActor = system.actorOf(Props(new MovieTitleActor(httpMock, testRootUrl)))
      titleActor ! imdbTestId
      expectMsg(MovieTitleAssigned(imdbTestId, imdbTestTitle))
    }
    "send http request and log error of server failure" in {
      val httpMock = httpMockWithResponse(HttpResponse(StatusCodes.InternalServerError))
      val titleActor = system.actorOf(Props(new MovieTitleActor(httpMock, testRootUrl)))
      EventFilter.error(message = "Request failed, response code: 500 Internal Server Error", occurrences = 1) intercept {
        titleActor ! "anything"
      }
    }
  }

  def httpMockWithResponse(response: HttpResponse): HttpExt = {
    val httpMock = mock[HttpExt]
    when(
      httpMock.singleRequest(
        any[HttpRequest],
        any[HttpsConnectionContext],
        any[ConnectionPoolSettings],
        any[LoggingAdapter]
      )
    ).thenReturn(Future(response))
    httpMock
  }

  def httpMockWithTitle(imdbId: String, title: String): HttpExt = {
    val httpHeaderMock = mock[HttpHeader]
    when(httpHeaderMock.name()).thenReturn("Entity-Id")
    when(httpHeaderMock.value()).thenReturn(imdbId)
    httpMockWithResponse(
      HttpResponse(StatusCodes.OK, List(httpHeaderMock), HttpEntity.apply(s"<title>$imdbTestTitle</title>"))
    )
  }
}

object MovieTitleActorSpec {
  val testRootUrl = "http://example.com"
  val imdbTestId = "imdbtestid"
  val imdbTestTitle = "imdbtesttitle"
}
