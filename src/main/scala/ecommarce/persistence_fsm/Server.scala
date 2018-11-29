package ecommarce.persistence_fsm

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, StandardRoute}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val paymentStatus: RootJsonFormat[OnlinePaymentStatus] = jsonFormat2(OnlinePaymentStatus)
}

object Server extends Directives with JsonSupport {
  val paypalBalance = new AtomicInteger(10)
  val bankBalance = new AtomicInteger(10)

  def decrementAndComplete(balance: AtomicInteger): StandardRoute = {
    balance.decrementAndGet()
    complete(OnlinePaymentStatus(status = true, balance.intValue()))
  }


  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("serverapp", config.getConfig("serverapp").withFallback(config))
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route = {
      path("paypal") {
        get {
          decrementAndComplete(paypalBalance)
        }
      }
      path("blik") {
        get {
          decrementAndComplete(bankBalance)
        }
      }
      path("cash") {
        get {
          println("Cash on delivery")
          complete(OnlinePaymentStatus(status = true, 0))
        }
      }
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 12345)
    println("Server online at localhost:12345")

    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}

case class OnlinePaymentStatus(status: Boolean, currentBalance: Int)

