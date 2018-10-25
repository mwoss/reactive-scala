package ecommarce.fsm_actors

import akka.actor.FSM
import FSMCheckout._
import akka.event.Logging
import ecommarce.fsm_actors.FSMCart.{CheckoutCanceled, CheckoutClosed}

import scala.concurrent.duration._

class FSMCheckout extends FSM[State, Data]{
  val logger = Logging(context.system, this)
  val payTimer = "payTimer"
  val chTimer = "chTimer"

  startWith(Unknown, NoneData)

  when(Unknown) {
    case Event(StartCheckout, NoneData) =>
      goto(DeliveryMethodSelect) using NoneData
  }
  when(DeliveryMethodSelect) {
    case Event(SelectedDeliveryMethod(method), NoneData) =>
      logger.info("Delivery method chosen: " + method)
      goto(PaymentMethodSelect) using CheckoutData(method, null)
  }
  when(PaymentMethodSelect){
    case Event(SelectedPaymentMethod(method), CheckoutData(delivery, _)) =>
      logger.info("Payment method chosen: " +  method)
      goto(PaymentProcess) using CheckoutData(delivery, method)
  }
  when(PaymentProcess){
    case Event(PaymentReceived, CheckoutData(delivery, payment)) =>
      logger.info("Payment done. Delivery {} and payment {}", delivery, payment)
      cancelTimer(payTimer)
      cancelTimer(chTimer)
      goto(PaymentClose)
  }
  when(PaymentClose){
    case Event(e, _) =>
      logger.info("Checkout closed")
      context.parent ! CheckoutClosed
      stay()
  }
  when(PaymentCancel){
    case Event(e, _) =>
      logger.info("Checkout canceled")
      context.parent ! CheckoutCanceled
      context stop self
      stay()
  }

  onTransition {
    case _ -> DeliveryMethodSelect =>
      setTimer(chTimer, CheckoutTimerExpired, 5 seconds)
    case _ -> PaymentMethodSelect =>
      cancelTimer(chTimer)
      setTimer(payTimer, PaymentTimerExpired, 5 seconds)
    case _ ->  (PaymentCancel | PaymentClose ) =>
      cancelTimer(chTimer)
      cancelTimer(payTimer)
  }

  whenUnhandled {
    case Event(PaymentTimerExpired, _) =>
      goto(PaymentClose)
    case Event(CheckoutTimerExpired, _) =>
      goto(PaymentClose)
    case Event(e, s) ⇒
      logger.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      context.parent ! CheckoutCanceled
      context stop self
      stay
  }
  initialize()
}

object FSMCheckout {
  sealed trait State
  case object Unknown extends State
  case object PaymentClose extends State
  case object PaymentCancel extends State
  case object DeliveryMethodSelect extends State
  case object PaymentMethodSelect extends State
  case object PaymentProcess extends State

  sealed trait Data
  case object NoneData extends Data
  case class CheckoutData(delivery: String, payment: String) extends Data

  sealed trait Command
  case object PaymentReceived extends Command
  case object StartCheckout extends Command
  case class SelectedPaymentMethod(method: String) extends Command
  case class SelectedDeliveryMethod(method: String) extends Command

  sealed trait CommandEvent
  case object CheckoutTimerExpired extends CommandEvent
  case object PaymentTimerExpired extends CommandEvent
}
