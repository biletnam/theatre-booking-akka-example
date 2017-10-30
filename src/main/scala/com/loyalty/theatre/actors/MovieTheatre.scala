package com.loyalty.theatre.actors

import akka.actor.{Actor, ActorLogging}

class MovieTheatre extends Actor with ActorLogging {
  override def receive: Receive = ???
}

object MovieTheatre {
  sealed trait Command
  final case class BookTicket(quantity: Int) extends Command
}
