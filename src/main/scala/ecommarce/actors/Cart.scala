package ecommarce.actors

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.{Logging, LoggingReceive}

import scala.concurrent.duration._
import Cart._

class Cart extends Actor with Timers {
  val log = Logging(context.system, this)

  private final case object CartTimer

  override def receive: Receive = empty

  def empty: Receive = {
    timers.cancelAll()
    LoggingReceive {
      case AddItem(item_id) =>
        context.become(nonEmpty(List(item_id)))
      case _ => log.info("You can only add an item to empty cart")
    }
  }

  def nonEmpty(items: List[String]): Receive = {
    timers.startSingleTimer(CartTimer, CartTimerExpired, 10 seconds)
    LoggingReceive {
      case AddItem(item_id) =>
        log.info("Added new item: " + item_id)
        context become nonEmpty(items :+ item_id)
      case RemoveItem(item_id) =>
        log.info(item_id + " removed")
        val withoutItem = items diff List(item_id)
        if (withoutItem.nonEmpty)
          context become nonEmpty(withoutItem)
        else
          context become empty
      case StartCheckout =>
        log.info("Starting checkout")
        val checkout = context.actorOf(Props(new Checkout(self)))
        sender() ! CheckoutStarted(checkout)
        context become inCheckout(items)
      case CartTimerExpired =>
        context become empty
      case CheckState =>
        log.info(items.toString())
        sender() ! items
    }
  }

  def inCheckout(items: List[String]): Receive = {
    LoggingReceive {
      case CheckoutCanceled =>
        log.info("Checkout canceled")
        context become nonEmpty(items)
      case CheckoutClosed =>
        log.info("Checkout closed")
        context become empty
      case CheckState =>
        sender() ! items
    }
  }
}

object Cart {
  sealed trait Command
  sealed trait Event

  case class AddItem(id: String) extends Command
  case class RemoveItem(id: String) extends Command
  case object StartCheckout extends Command
  case object CheckState extends Command

  case class CheckoutStarted(checkout: ActorRef) extends Event
  case object CheckoutClosed extends Event
  case object CheckoutCanceled extends Event
  case object CartTimerExpired extends Event
}
