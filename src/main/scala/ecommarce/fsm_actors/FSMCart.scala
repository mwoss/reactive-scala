package ecommarce.fsm_actors

import akka.actor.{ActorRef, FSM, Props}
import FSMCart._
import akka.event.Logging

import scala.concurrent.duration._

class FSMCart extends FSM[State, Data]{
  val logger = Logging(context.system, this)

  startWith(Empty, EmptyCart)

  when(Empty){
    case Event(AddItem(item), EmptyCart) =>
      log.info("Added new item to empty cart: " + item)
      goto(NonEmpty) using NonEmptyCart(List(item))
  }

  when(NonEmpty){
    case Event(AddItem(item), NonEmptyCart(items)) =>
      log.info("Added new item to cart: " + item)
      goto(NonEmpty) using NonEmptyCart(items :+ item)
    case Event(RemoveItem(item), NonEmptyCart(items)) =>
      log.info(item + " removed")
      val withoutItem = items diff List(item)
      if (withoutItem.nonEmpty)
        goto(NonEmpty) using NonEmptyCart(withoutItem)
      else
        goto(Empty) using EmptyCart
    case Event(StartCheckout, NonEmptyCart(items)) =>
      log.info("Starting checkout")
      val checkoutActor = context.actorOf(Props[FSMCheckout], "checkout")
      checkoutActor ! StartCheckout
      sender() ! CheckoutStarted(checkoutActor)
      goto(InCheckout) using NonEmptyCart(items)
    case Event(CartTimerExpired, _) =>
      goto(Empty) using EmptyCart
  }

  when(InCheckout){
    case Event(CheckoutCanceled, NonEmptyCart(_)) =>
      log.info("Checkout canceled")
      goto(Empty) using EmptyCart
    case Event(CheckoutClosed, NonEmptyCart(_)) =>
      log.info("Checkout closed")
      goto(Empty) using EmptyCart
  }

  onTransition {
    case _ -> NonEmpty => setTimer("timer", CartTimerExpired, 10 seconds)
    case _ -> (Empty | InCheckout) => cancelTimer("timer")
  }

  whenUnhandled {
    case Event(e, s) ⇒
      logger.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}

object FSMCart {
  sealed trait State
  case object Empty extends State
  case object NonEmpty extends State
  case object InCheckout extends State

  sealed trait Data
  case object EmptyCart extends Data
  case class NonEmptyCart(items: List[String]) extends Data

  sealed trait Command
  case class AddItem(id: String) extends Command
  case class RemoveItem(id: String) extends Command
  case object StartCheckout extends Command
  case object CheckState extends Command

  sealed trait CommandEvent
  case class CheckoutStarted(checkout: ActorRef) extends CommandEvent
  case object CheckoutClosed extends CommandEvent
  case object CheckoutCanceled extends CommandEvent
  case object CartTimerExpired extends CommandEvent
}