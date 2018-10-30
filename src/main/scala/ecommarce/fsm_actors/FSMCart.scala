package ecommarce.fsm_actors

import akka.actor.{FSM, Props}
import FSMCart._
import akka.event.Logging
import ecommarce.fsm_actors.FSMOrderManager.{Empty => _, InCheckout => _, OrderManagerData => _, OrderManagerState => _, _}
import ecommarce.messages._
import ecommarce.utils.StringItem

import scala.concurrent.duration._

class FSMCart extends FSM[State, Data]{
  val logger = Logging(context.system, this)

  startWith(Empty, EmptyCart)

  when(Empty){
    case Event(AddItem(item), EmptyCart) =>
      log.info("Added new item to empty cart: " + item)
      context.parent ! ItemAdded
      goto(NonEmpty) using NonEmptyCart(Set(item))
  }

  when(NonEmpty){
    case Event(AddItem(item), NonEmptyCart(items)) =>
      log.info("Added new item to cart: " + item)
      sender() ! ItemAdded
      stay using NonEmptyCart(items + item)
    case Event(RemoveItem(item), NonEmptyCart(items)) =>
      log.info(item + " removed")
      val withoutItem = items - item
      sender() ! ItemRemoved
      if (withoutItem.nonEmpty)
        stay using NonEmptyCart(withoutItem)
      else
        goto(Empty) using EmptyCart
    case Event(CheckState, NonEmptyCart(items)) =>
      logger.info("Item in cart {}", items.toString())
      sender() ! items
      stay using NonEmptyCart(items)
    case Event(StartCheckout, NonEmptyCart(items)) =>
      log.info("Starting checkout")
      val checkoutActor = context.actorOf(Props[FSMCheckout], "checkout")
      checkoutActor ! StartCheckout
      sender() ! CheckoutStarted(checkoutActor)
      goto(InCheckout) using NonEmptyCart(items)
  }

  when(InCheckout){
    case Event(CheckoutCanceled, NonEmptyCart(_)) =>
      log.info("Checkout canceled")
      goto(Empty) using EmptyCart
    case Event(CheckoutClosed, NonEmptyCart(_)) =>
      log.info("Checkout closed")
      context.parent ! CartEmptied
      goto(Empty) using EmptyCart
  }

  onTransition {
    case _ -> NonEmpty => setTimer("timer", CartTimerExpired, 3 seconds)
    case _ -> (Empty | InCheckout) => cancelTimer("timer")
  }

  whenUnhandled {
    case Event(CartTimerExpired, _) =>
      goto(Empty) using EmptyCart
    case Event(e, s) =>
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
  case class NonEmptyCart(items: Set[StringItem]) extends Data
}