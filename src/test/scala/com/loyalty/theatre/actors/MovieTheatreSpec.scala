package com.loyalty.theatre.actors

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.loyalty.theatre.StopActorSystemAfterAll
import org.scalatest.FunSpecLike

class MovieTheatreSpec
    extends TestKit(ActorSystem("movie-theatre-test-system"))
    with FunSpecLike
    with ImplicitSender
    with StopActorSystemAfterAll {
  describe("Movie Theatre") {
    import MovieTheatre._

    it("must allow a user to book a ticket when a booking does not exceed ticket capacity") {
      val movieTheatre = system.actorOf(props(maxSeatingCapacity = 10))
      val numberOfTickets = 5
      movieTheatre ! BookTicket(quantity = numberOfTickets)
      expectMsg(TicketBooked(quantity = numberOfTickets))
    }

    it("must prevent a user from over-booking tickets") {
      val movieTheatre = system.actorOf(props(maxSeatingCapacity = 10))
      val numberOfTickets = 11
      movieTheatre ! BookTicket(quantity = numberOfTickets)
      expectMsg(NotEnoughSeats)
    }

    it("must prevent users from booking tickets when the theatre has reached its capacity") {
      val movieTheatre = system.actorOf(props(maxSeatingCapacity = 10))
      val numberOfTickets = 10
      movieTheatre ! BookTicket(quantity = numberOfTickets)
      expectMsg(TicketBooked(quantity = numberOfTickets))

      // the next person to book a ticket gets denied
      movieTheatre ! BookTicket(quantity = 1)
      expectMsg(CapacityReached)
    }
  }
}
