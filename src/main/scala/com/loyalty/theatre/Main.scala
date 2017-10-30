package com.loyalty.theatre

import akka.actor.ActorSystem

object Main extends App {
  val system: ActorSystem = ActorSystem("movie-theatre-ticketing-system")
}
