package ecommarce.pure_fsm

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ecommarce.pure_fsm.normal_actors.Cart
import ecommarce.pure_fsm.normal_actors.Cart._
import ecommarce.pure_fsm.normal_actors.Checkout._

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