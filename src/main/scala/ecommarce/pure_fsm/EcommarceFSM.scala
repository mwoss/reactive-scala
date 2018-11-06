package ecommarce.pure_fsm

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import ecommarce.pure_fsm.fsm_actors.FSMOrderManager
import ecommarce.pure_fsm.messages._
import ecommarce.pure_fsm.utils.{StringDelivery, StringItem, StringPayment}

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