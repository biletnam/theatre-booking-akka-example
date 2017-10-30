package com.loyalty.theatre.actors

import java.util.UUID

import akka.actor.{Actor, ActorLogging}

class MovieTheatre(seatingCapacity: Int) extends Actor with ActorLogging {
  override def receive: Receive = ???
}

object MovieTheatre {
  case class State(remainingSeats: Int)

  sealed trait Command
  final case class BookTicket(quantity: Int) extends Command

  sealed trait Event
  final case class TicketBooked(quantity: Int, receipt: UUID) extends Event
}
