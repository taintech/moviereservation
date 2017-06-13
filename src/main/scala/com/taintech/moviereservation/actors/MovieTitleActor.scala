package com.taintech.moviereservation.actors

import akka.actor.{ Actor, ActorLogging, ActorRef }
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.taintech.moviereservation.actors.ReservationActor.MovieTitleAssigned
import org.jsoup.Jsoup

import scala.concurrent.ExecutionContextExecutor

class MovieTitleActor(
  http: HttpExt,
  imdbUrlRoot: String
)(implicit val materializer: ActorMaterializer, implicit val executionContext: ExecutionContextExecutor)
    extends Actor
    with ActorLogging {
  import akka.pattern.pipe

  override def receive: Receive = {
    case imdbId: String =>
      val originalSender = sender()
      val futureResponse = http.singleRequest(HttpRequest(uri = s"$imdbUrlRoot/$imdbId/"))
      futureResponse.map(r => (originalSender, r)).pipeTo(self)
    case (originalSender: ActorRef, HttpResponse(StatusCodes.OK, headers, entity, _)) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        val html = body.utf8String
        log.debug("Got response, body: " + html)
        val doc = Jsoup.parse(html)
        originalSender ! MovieTitleAssigned(
          headers.find(_.name() == "Entity-Id").get.value(),
          doc.title().split("\\(")(0).trim
        )
      }
    case (originalSender: ActorRef, resp @ HttpResponse(code, _, _, _)) =>
      log.error("Request failed, response code: " + code)
      resp.discardEntityBytes()
  }
}
