package ecommarce.fsm_actors

import akka.actor.{ActorRef, FSM, Props}
import FSMOrderManager._
import akka.event.Logging
import ecommarce.messages._

class FSMOrderManager extends FSM[OrderManagerState, OrderManagerData] {
  val logger = Logging(context.system, this)


  startWith(Uninitialized, Empty)

  when(Uninitialized) {
    case Event(StartShopping, _) =>
      logger.info("Order manager started")
      val cartActor: ActorRef = context.actorOf(Props[FSMCart], "cart")
      sender() ! Done
      goto(Open) using CartData(cartActor)
  }

  when(Open) {
    case Event(AddItem(item), CartData(cartRef)) =>
      cartRef ! AddItem(item)
      stay using CartDataWithSender(cartRef, sender())
    case Event(ItemAdded, CartDataWithSender(cartRef, senderRef)) =>
      logger.info("New item registered")
      senderRef ! Done
      stay using CartData(cartRef)
    case Event(RemoveItem(item), CartData(cartRef)) =>
      cartRef ! RemoveItem(item)
      stay using CartDataWithSender(cartRef, sender())
    case Event(ItemRemoved, CartDataWithSender(cartRef, senderRef)) =>
      logger.info("Item removal registered")
      senderRef ! Done
      stay using CartData(cartRef)
    case Event(CheckState, CartData(cartRef)) =>
      cartRef ! CheckState
      stay using CartData(cartRef)
    case Event(Buy, CartData(cartRef)) =>
      logger.info("Buying items")
      cartRef ! StartCheckout
      stay using CartDataWithSender(cartRef, sender())
    case Event(CheckoutStarted(checkoutRef), CartDataWithSender(cartRef, senderRef)) =>
      logger.info("Checkout start registered")
      senderRef ! Done
      goto(InCheckout) using InCheckoutData(checkoutRef)
  }

  when(InCheckout) {
    case Event(SelectDeliveryMethod(delivery), InCheckoutData(checkoutRef)) =>
      checkoutRef ! SelectDeliveryMethod(delivery)
      stay using InCheckoutDataWithSender(checkoutRef, sender())
    case Event(DeliveryMethodSelected, InCheckoutDataWithSender(checkoutRef, senderRef)) =>
      logger.info("Delivery method registered")
      senderRef ! Done
      stay using InCheckoutData(checkoutRef)
    case Event(SelectPaymentMethod(payment), InCheckoutData(checkoutRef)) =>
      checkoutRef ! SelectPaymentMethod(payment)
      stay using InCheckoutDataWithSender(checkoutRef, sender())
    case Event(PaymentServiceStarted(paymentRef), InCheckoutDataWithSender(checkoutRef, senderRef)) =>
      logger.info("Payment method registered")
      senderRef ! Done
      goto(InPayment) using InPaymentData(paymentRef)
  }

  when(InPayment) {
    case Event(Pay, InPaymentData(paymentRef)) =>
      paymentRef ! Pay
      stay using InPaymentDataWithSender(paymentRef, sender())
    case Event(PaymentConfirmed, InPaymentDataWithSender(paymentRef, senderRef)) =>
      logger.info("Finished payment registered")
      senderRef ! Done
      goto(Finished)
  }

  when(Finished) {
    case Event(CartEmptied, _) =>
      logger.info("Order finished")
      stay
  }

  whenUnhandled {
    case Event(e, s) =>
      logger.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}

object FSMOrderManager {

  trait OrderManagerState
  case object Uninitialized extends OrderManagerState
  case object Open extends OrderManagerState
  case object InCheckout extends OrderManagerState
  case object InPayment extends OrderManagerState
  case object Finished extends OrderManagerState

  trait OrderManagerData
  case object Empty extends OrderManagerData
  case class CartData(cartRef: ActorRef) extends OrderManagerData
  case class CartDataWithSender(cartRef: ActorRef, sender: ActorRef) extends OrderManagerData
  case class InCheckoutData(checkoutRef: ActorRef) extends OrderManagerData
  case class InCheckoutDataWithSender(checkoutRef: ActorRef, sender: ActorRef) extends OrderManagerData
  case class InPaymentData(paymentRef: ActorRef) extends OrderManagerData
  case class InPaymentDataWithSender(paymentRef: ActorRef, sender: ActorRef) extends OrderManagerData

}
