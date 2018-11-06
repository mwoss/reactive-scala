package ecommarce.pure_fsm.messages

import akka.actor.ActorRef
import ecommarce.pure_fsm.utils.{StringDelivery, StringItem, StringPayment}

// Order Manager messages
trait OrderManagerCommand
case object StartShopping extends OrderManagerCommand
case class AddItem(item: StringItem) extends OrderManagerCommand
case class RemoveItem(item: StringItem) extends OrderManagerCommand
case object CheckState extends OrderManagerCommand
case object StartCheckout extends OrderManagerCommand
case class SelectDeliveryMethod(delivery: StringDelivery) extends OrderManagerCommand
case class SelectPaymentMethod(payment: StringPayment) extends OrderManagerCommand
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

// Checkout messages
sealed trait CheckoutCommand
case object StartPayment extends CheckoutCommand

sealed trait CheckoutEvent
case class PaymentServiceStarted(paymentRef: ActorRef) extends CheckoutEvent
case object DeliveryMethodSelected extends CheckoutEvent
case object CheckoutTimerExpired extends CheckoutEvent
case object PaymentTimerExpired extends CheckoutEvent

// Payment messages
sealed trait PaymentEvent
case object PaymentConfirmed extends PaymentEvent
case object PaymentReceived extends PaymentEvent