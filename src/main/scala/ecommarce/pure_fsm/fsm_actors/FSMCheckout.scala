package ecommarce.pure_fsm.fsm_actors

import akka.actor.{FSM, Props}
import FSMCheckout._
import akka.event.Logging
import ecommarce.pure_fsm.messages._
import ecommarce.pure_fsm.utils.{StringDelivery, StringPayment}

import scala.concurrent.duration._

class FSMCheckout extends FSM[State, Data]{
  val logger = Logging(context.system, this)
  val payTimer = "payTimer"
  val chTimer = "chTimer"

  startWith(Uninitialized, NoneData)

  when(Uninitialized) {
    case Event(StartCheckout, NoneData) =>
      goto(DeliveryMethodSelect) using NoneData
  }
  when(DeliveryMethodSelect) {
    case Event(SelectDeliveryMethod(method), NoneData) =>
      logger.info("Delivery method chosen: " + method)
      sender() ! DeliveryMethodSelected
      goto(PaymentMethodSelect) using CheckoutData(method, StringPayment(""))
  }
  when(PaymentMethodSelect){
    case Event(SelectPaymentMethod(method), CheckoutData(delivery, _)) =>
      logger.info("Payment method chosen: " +  method)
      val paymentActor = context.actorOf(Props[FSMPayment], "payment")
      paymentActor ! StartPayment
      sender() ! PaymentServiceStarted(paymentActor)
      goto(PaymentProcess) using CheckoutData(delivery, method)
  }
  when(PaymentProcess){
    case Event(PaymentReceived, CheckoutData(delivery, payment)) =>
      logger.info("Payment done. Delivery {}, payment {}", delivery, payment)
      cancelTimer(payTimer)
      cancelTimer(chTimer)
      logger.info("Checkout closed")
      context.parent ! CheckoutClosed
      stay()
  }

  onTransition {
    case _ -> DeliveryMethodSelect =>
      setTimer(chTimer, CheckoutTimerExpired, 3 seconds)
    case _ -> PaymentMethodSelect =>
      cancelTimer(chTimer)
      setTimer(payTimer, PaymentTimerExpired, 3 seconds)
  }

  whenUnhandled {
    case Event(PaymentTimerExpired, _) | Event(CheckoutTimerExpired, _) =>
      logger.info("Checkout canceled")
      context.parent ! CheckoutCanceled
      context stop self
      stay()
    case Event(e, s) =>
      logger.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }
  initialize()
}

object FSMCheckout {
  sealed trait State
  case object Uninitialized extends State
  case object PaymentClose extends State
  case object PaymentCancel extends State
  case object DeliveryMethodSelect extends State
  case object PaymentMethodSelect extends State
  case object PaymentProcess extends State

  sealed trait Data
  case object NoneData extends Data
  case class CheckoutData(delivery: StringDelivery, payment: StringPayment) extends Data
}
