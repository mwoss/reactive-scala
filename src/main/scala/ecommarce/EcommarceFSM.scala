package ecommarce

import akka.actor.{ActorRef, ActorSystem, Props}
import ecommarce.fsm_actors.FSMOrderManager
import akka.pattern.ask
import akka.util.Timeout
import ecommarce.messages._
import ecommarce.utils.{StringDelivery, StringItem, StringPayment}

import scala.concurrent.Await
import scala.concurrent.duration._

object EcommarceFSM extends App {
  implicit val timeout: Timeout = 30 seconds

  val system = ActorSystem("Ecommerce")
  val orderActor: ActorRef = system.actorOf(Props[FSMOrderManager], "managerActor")

  Await.result(orderActor ? StartShopping, timeout.duration)

  Await.result(orderActor ? AddItem(StringItem("notebook")), timeout.duration)
  Await.result(orderActor ? Buy, timeout.duration)

  Await.result(orderActor ? SelectDeliveryMethod(StringDelivery("fedex")), timeout.duration)
  Await.result(orderActor ? SelectPaymentMethod(StringPayment("blik")), timeout.duration)

  Await.result(orderActor ? Pay, timeout.duration)
}