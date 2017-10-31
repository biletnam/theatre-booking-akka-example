package com.loyalty.theatre.actors

import akka.actor.{Actor, ActorLogging}

class MovieTheatre(seatingCapacity: Int) extends Actor with ActorLogging {
  import com.loyalty.theatre.actors.MovieTheatre._

  def theatreAtCapacity: Receive = {
    case _: BookTicket => sender() ! CapacityReached
  }

  def theatreWithCapacity(remaining: Int): Receive = {
    case BookTicket(quantity) if quantity > remaining =>
      sender() ! NotEnoughSeats

    case BookTicket(quantity) if quantity == remaining =>
      sender() ! TicketBooked(quantity)
      context become theatreAtCapacity

    case BookTicket(quantity) =>
      sender() ! TicketBooked(quantity)
      context become theatreWithCapacity(remaining - quantity)
  }

  // initial state
  override def receive: Receive = theatreWithCapacity(seatingCapacity)
}

object MovieTheatre {
  case class State(remainingSeats: Int)

  sealed trait Command
  final case class BookTicket(quantity: Int) extends Command

  sealed trait Response
  final case class TicketBooked(quantity: Int) extends Response
  final case object NotEnoughSeats extends Response
  final case object CapacityReached extends Response
}
