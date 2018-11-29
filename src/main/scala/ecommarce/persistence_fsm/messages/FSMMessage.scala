package ecommarce.persistence_fsm.messages

import java.net.URI

import akka.actor.ActorRef
import ecommarce.persistence_fsm.utils.{Item, PaymentMethodType, StringDelivery}

// Order Manager messages
trait OrderManagerCommand
case object StartShopping extends OrderManagerCommand
case class AddItem(item: Item) extends OrderManagerCommand
case class RemoveItem(itemID: URI, quantity: Int) extends OrderManagerCommand
case object CheckState extends OrderManagerCommand
case object StartCheckout extends OrderManagerCommand
case class SelectDeliveryMethod(delivery: StringDelivery) extends OrderManagerCommand
case class SelectPaymentMethod(payment: PaymentMethodType) extends OrderManagerCommand
case object Buy extends OrderManagerCommand
case object Pay extends OrderManagerCommand

trait OrderManagerEvent
case object Done extends OrderManagerEvent

// Cart messages
sealed trait CartEvent
case class CheckoutStarted(checkout: ActorRef) extends CartEvent
case object CheckoutClosed extends CartEvent
case object CheckoutCanceled extends CartEvent
case object CartTimerExpired extends CartEvent
case object CartEmptied extends CartEvent
case object ItemAdded extends CartEvent
case object ItemRemoved extends CartEvent
case class StateChecked(items: Map[URI, Item]) extends CartEvent

// Checkout messages
sealed trait CheckoutCommand
case class StartPayment(method: PaymentMethodType) extends CheckoutCommand

sealed trait CheckoutEvent
case class PaymentServiceStarted(paymentRef: ActorRef) extends CheckoutEvent
case object DeliveryMethodSelected extends CheckoutEvent
case object CheckoutTimerExpired extends CheckoutEvent
case object PaymentTimerExpired extends CheckoutEvent

// Payment messages
sealed trait PaymentEvent
case object PaymentConfirmed extends PaymentEvent
case object PaymentReceived extends PaymentEvent
case class ProceedPayment(method: PaymentMethodType) extends PaymentEvent