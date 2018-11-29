package ecommarce.persistence_fsm.fsm_actors

import akka.actor.Props
import FSMCheckout._
import akka.event.Logging
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import ecommarce.persistence_fsm.messages._
import ecommarce.persistence_fsm.utils.{PaymentMethodType, StringDelivery, StringPayment}

import scala.concurrent.duration._
import scala.reflect._

class FSMCheckout(checkoutID: String) extends PersistentFSM[State, CheckoutData, CheckoutDomainEvent]{
  override def domainEventClassTag: ClassTag[CheckoutDomainEvent] = classTag[CheckoutDomainEvent]
  override def persistenceId: String = checkoutID

  val logger = Logging(context.system, this)
  val payTimer = "payTimer"
  val checkoutTimer = "chTimer"
  val timeout: FiniteDuration = 3.seconds

  startWith(Uninitialized, CheckoutData())

  when(Uninitialized) {
    case Event(StartCheckout, _) =>
      goto(DeliveryMethodSelect) applying StartCheckoutDomainEvent
  }
  when(DeliveryMethodSelect) {
    case Event(SelectDeliveryMethod(method), _) =>
      logger.info("Delivery method chosen: " + method)
      goto(PaymentMethodSelect) applying SelectDeliveryMethodDomainEvent(method) replying DeliveryMethodSelected
  }
  when(PaymentMethodSelect){
    case Event(SelectPaymentMethod(method), _) =>
      logger.info("Payment method chosen: " +  method)
      val paymentActor = context.actorOf(Props[FSMPayment], "payment")
      paymentActor ! StartPayment(method)
      goto(PaymentProcess) applying SelectPaymentMethodDomainEvent(method) replying PaymentServiceStarted(paymentActor)
  }
  when(PaymentProcess){
    case Event(PaymentReceived, CheckoutData(delivery, payment)) =>
      logger.info("Payment done. Delivery {}, payment {}", delivery, payment)
      context.parent ! CheckoutClosed
      stay applying FinishCheckoutDomainEvent
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


  override def applyEvent(domainEvent: CheckoutDomainEvent, currentData: CheckoutData): CheckoutData = {
    domainEvent match {
      case StartCheckoutDomainEvent =>
        setTimer(checkoutTimer, CheckoutTimerExpired, timeout)
        currentData
      case SelectDeliveryMethodDomainEvent(method) =>
        currentData.copy(delivery = Some(method))
      case SelectPaymentMethodDomainEvent(method) =>
        cancelTimer(checkoutTimer)
        setTimer(payTimer, PaymentTimerExpired, timeout)
        currentData.copy(payment = Some(method))
      case FinishCheckoutDomainEvent =>
        logger.info("Checkout closed - checkout")
        cancelTimer(payTimer)
        context stop self
        currentData
    }
  }
}

object FSMCheckout {
  sealed trait State extends FSMState
  case object Uninitialized extends State {
    override def identifier: String = "Uninitialized"
  }
  case object PaymentClose extends State {
    override def identifier: String = "PaymentClose"
  }
  case object PaymentCancel extends State {
    override def identifier: String = "PaymentCancel"
  }
  case object DeliveryMethodSelect extends State {
    override def identifier: String = "DeliveryMethodSelect"
  }
  case object PaymentMethodSelect extends State {
    override def identifier: String = "PaymentMethodSelect"
  }
  case object PaymentProcess extends State {
    override def identifier: String = "PaymentProcess"
  }

  sealed trait Data
  case class CheckoutData(delivery: Option[StringDelivery] = None,
                          payment: Option[PaymentMethodType] = None) extends Data

  sealed trait CheckoutDomainEvent
  case object StartCheckoutDomainEvent extends CheckoutDomainEvent
  case class SelectDeliveryMethodDomainEvent(method: StringDelivery) extends CheckoutDomainEvent
  case class SelectPaymentMethodDomainEvent(method: PaymentMethodType) extends CheckoutDomainEvent
  case object FinishCheckoutDomainEvent extends CheckoutDomainEvent
}
