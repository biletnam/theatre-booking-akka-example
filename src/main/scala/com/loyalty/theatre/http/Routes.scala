package com.loyalty.theatre.http

import akka.actor.ActorRef
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import akka.pattern.ask
import com.loyalty.theatre.actors.MovieTheatre._
import com.loyalty.theatre.actors.sharding.MovieTheatreSettings.MovieTheatreEnvelope

import scala.util.{Failure, Success}

trait Routes extends SprayJsonSupport {
  implicit val timeout: Timeout
  val theatreCoordinator: ActorRef
  val log: LoggingAdapter

  val routes: Route = (post & path("theatres" / Segment / "tickets" / IntNumber)) {
    (theatreName: String, ticketsToBook: Int) =>
      val response = theatreCoordinator ? MovieTheatreEnvelope(theatreName, BookTicket(quantity = ticketsToBook))
      onComplete(response.mapTo[Response]) {
        case Success(TicketBooked(bookedQuantity)) =>
          complete(Created, s"$bookedQuantity")

        case Success(NotEnoughSeats) =>
          complete(Forbidden, s"Not Enough Seats")

        case Success(CapacityReached) =>
          complete(Forbidden, s"Theatre is at capacity")

        case Failure(exception) =>
          log.error(exception, "Failed to book tickets")
          complete(ServiceUnavailable)
      }
  }
}
