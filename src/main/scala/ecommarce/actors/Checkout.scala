package ecommarce.actors

import akka.actor.{Actor, ActorRef, Timers}
import akka.event.{Logging, LoggingReceive}
import ecommarce.fsm_actors.FSMCart.{CheckoutCanceled, CheckoutClosed}

import scala.concurrent.duration._
import ecommarce.actors.Checkout._

class Checkout(checkoutCart: ActorRef) extends Actor with Timers {
  val log = Logging(context.system, self)

  private final case object PaymentTimer
  private final case object CheckoutTimer

  override def receive: Receive = selectDeliveryMethod

  def closePayment: Receive = {
    timers.cancelAll()
    LoggingReceive {
      case _ =>
        log.info("Checkout closed")
        context.parent ! CheckoutClosed
        context stop self
    }
  }

  def cancelPayment: Receive = {
    timers.cancelAll()
    LoggingReceive {
      case _ =>
        log.info("Checkout canceled")
        context.parent ! CheckoutCanceled
        context stop self
    }
  }

  def selectDeliveryMethod: Receive = {
    timers.startSingleTimer(CheckoutTimer, CheckoutTimerExpired, 10 seconds)
    LoggingReceive {
      case SelectedDeliveryMethod(method) =>
        log.info("Delivery method chosen: " + method)
        context become selectPaymentMethod
      case CheckoutCanceled =>
        context become cancelPayment
      case CheckoutTimerExpired =>
        context become cancelPayment
    }
  }

  def selectPaymentMethod: Receive = {
    LoggingReceive {
      case SelectedPaymentMethod(method) =>
        log.info("Method chosen: " +  method)
        context become processPayment
      case CheckoutTimerExpired =>
        context become cancelPayment
      case CheckoutCanceled =>
        context become cancelPayment
    }
  }

  def processPayment: Receive = {
    timers.cancel(CheckoutTimer)
    timers.startSingleTimer(PaymentTimer, PaymentTimerExpired, 10 seconds)
    LoggingReceive {
      case PaymentReceived =>
        checkoutCart ! CheckoutClosed
        context become closePayment
      case PaymentTimerExpired =>
        context become cancelPayment
      case CheckoutCanceled =>
        context become cancelPayment
    }
  }
}


object Checkout {
  sealed trait Command
  sealed trait Event
  case object PaymentReceived extends Command
  case class SelectedPaymentMethod(method: String) extends Command
  case class SelectedDeliveryMethod(method: String) extends Command

  case object CheckoutTimerExpired extends Event
  case object PaymentTimerExpired extends Event
}
