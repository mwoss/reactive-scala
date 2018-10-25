package ecommarce

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ecommarce.fsm_actors.FSMCart
import ecommarce.fsm_actors.FSMCart.{AddItem, CheckoutStarted, StartCheckout}
import ecommarce.fsm_actors.FSMCheckout.{PaymentReceived, SelectedDeliveryMethod, SelectedPaymentMethod}

object EcommarceFSM extends App{
  val system = ActorSystem("Ecommerce")
  system.actorOf(Props[MainActorFSM], "cartActor")
}

class MainActorFSM extends Actor {
  val cart: ActorRef = context.actorOf(Props[FSMCart], "cart")

  cart ! AddItem("socks")

  Thread.sleep(12* 1000)

  cart ! AddItem("tshirt")
  cart ! AddItem("jeans")
  cart ! StartCheckout

  override def receive: Receive = {
    case CheckoutStarted(checkout) =>
      checkout ! SelectedDeliveryMethod("post")
      checkout ! SelectedPaymentMethod("online bank transfer")
      checkout ! PaymentReceived
  }
}