package ecommarce.persistence_fsm

import java.net.URI

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import ecommarce.persistence_fsm.fsm_actors.FSMOrderManager
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import ecommarce.persistence_fsm.ProductCatalog.GetItems
import ecommarce.persistence_fsm.messages._
import ecommarce.persistence_fsm.utils.{Blik, Item, StringDelivery}

import scala.concurrent.Await
import scala.concurrent.duration._

object EcommarceFSM extends App {
  implicit val timeout: Timeout = 30 seconds

  def classic() = {
    val system = ActorSystem("Ecommerce")
    val orderActor: ActorRef = system.actorOf(Props[FSMOrderManager], "managerActor")

    Await.result(orderActor ? StartShopping, timeout.duration)

    Await.result(orderActor ? AddItem(Item(new URI("notebook"), "notebook", BigDecimal(2500), 2)), timeout.duration)
    Await.result(orderActor ? Buy, timeout.duration)

    Await.result(orderActor ? SelectDeliveryMethod(StringDelivery("fedex")), timeout.duration)
    Await.result(orderActor ? SelectPaymentMethod(Blik), timeout.duration)

    Await.result(orderActor ? Pay, timeout.duration)
  }

  def productCatalog() = {
    val config = ConfigFactory.load()

    val clientsystem = ActorSystem("Reactive5", config.getConfig("clientapp").withFallback(config))
    val client = clientsystem.actorOf(Props[Client], "client")

    Await.result(client ? GetItems("starbucks", List("coffee", "drink")), timeout.duration)

    clientsystem.terminate()
    Await.result(clientsystem.whenTerminated, Duration.Inf)
  }
}

class Client extends Actor {
  implicit val timeout: Timeout = Timeout(5 seconds)

  def receive = LoggingReceive {
    case GetItems(brand, keywords) =>
      val catalog = context.actorSelection("akka.tcp://ProductCatalog@127.0.0.1:2552/user/catalog")
      val result = Await.result(catalog ? GetItems(brand, keywords), timeout.duration)
      sender() ! "done"
  }
}