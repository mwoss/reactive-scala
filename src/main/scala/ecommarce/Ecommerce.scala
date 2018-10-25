package ecommarce

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ecommarce.actors.Cart
import ecommarce.actors.Cart.{AddItem, CheckState, CheckoutStarted, StartCheckout}
import ecommarce.actors.Checkout.{PaymentReceived, SelectedDeliveryMethod, SelectedPaymentMethod}

object Ecommerce extends App{
  val system = ActorSystem("Ecommerce")
  system.actorOf(Props[MainActor], "cartActor")
}

class MainActor extends Actor {
  val cart: ActorRef = context.actorOf(Props[Cart], "cart")

  cart ! AddItem("socks")
  cart ! CheckState

  Thread.sleep(12* 1000)

  cart ! AddItem("tshirt")
  cart ! AddItem("jeans")
  cart ! CheckState
  cart ! StartCheckout

  override def receive: Receive = {
    case CheckoutStarted(checkout) =>
      checkout ! SelectedDeliveryMethod("post")
      checkout ! SelectedPaymentMethod("online bank transfer")
      checkout ! PaymentReceived
  }
}