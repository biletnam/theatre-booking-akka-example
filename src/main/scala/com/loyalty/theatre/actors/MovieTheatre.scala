package com.loyalty.theatre.actors

import akka.actor.{ Actor, ActorLogging, Props }

class MovieTheatre(maxSeatingCapacity: Int) extends Actor with ActorLogging {
  import com.loyalty.theatre.actors.MovieTheatre._

  def theatreAtCapacity: Receive = {
    case _: BookTicket => sender() ! CapacityReached
  }

  def theatreWithCapacity(remaining: Int): Receive = {
    case BookTicket(quantity) if quantity > remaining =>
      sender() ! NotEnoughSeats

    case BookTicket(quantity) if quantity == remaining =>
      sender() ! TicketBooked(quantity)
      log.info("Theatre {} is fully booked", self.path.name)
      context become theatreAtCapacity

    case BookTicket(quantity) =>
      sender() ! TicketBooked(quantity)
      val updatedRemaining = remaining - quantity
      log.debug("Theatre {} has {} tickets remaining", self.path.name, updatedRemaining)
      context become theatreWithCapacity(updatedRemaining)
  }

  // initial state
  override def receive: Receive = theatreWithCapacity(maxSeatingCapacity)
}

object MovieTheatre {
  case class State(remainingSeats: Int)

  sealed trait Command
  final case class BookTicket(quantity: Int) extends Command

  sealed trait Response
  final case class TicketBooked(quantity: Int) extends Response
  final case object NotEnoughSeats extends Response
  final case object CapacityReached extends Response

  def props(maxSeatingCapacity: Int = 50): Props = Props(new MovieTheatre(maxSeatingCapacity))
}
