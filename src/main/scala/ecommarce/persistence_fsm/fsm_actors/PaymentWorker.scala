package ecommarce.persistence_fsm.fsm_actors


import akka.actor.{Actor, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import ecommarce.persistence_fsm.{JsonSupport, OnlinePaymentStatus}
import ecommarce.persistence_fsm.messages.ProceedPayment
import ecommarce.persistence_fsm.utils.{Blik, Cash, PayPal}
import spray.json._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class PaymentWorker extends Actor with SprayJsonSupport with DefaultJsonProtocol with JsonSupport {
  import akka.pattern.pipe
  import context.dispatcher

  println("Ready to serve")

  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  val http = Http(context.system)

  def receive: PartialFunction[Any, Unit] = {
    case ProceedPayment(method) if method == PayPal =>
      println("Paypal")
      http.singleRequest(HttpRequest(uri = "http://localhost:8080/paypal")).pipeTo(self)

    case ProceedPayment(method) if method == Blik =>
      println("Blik")
      http.singleRequest(HttpRequest(uri = "http://localhost:8080/blick")).pipeTo(self)

    case ProceedPayment(method) if method == Cash =>
      println("Cash on delivery")
      http.singleRequest(HttpRequest(uri = "http://localhost:8080/cash")).pipeTo(self)

    case resp@HttpResponse(code, _, _, _) if code != StatusCodes.OK =>
      throw new IllegalResponseException(ErrorInfo(s"Status code: $code"))

    case resp@HttpResponse(StatusCodes.OK, headers, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        context.parent ! body.utf8String.parseJson.convertTo[OnlinePaymentStatus]
        resp.discardEntityBytes()
        shutdown()
      }
    case resp@HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      shutdown()

  }

  def shutdown(): Future[Terminated] = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }
}

sealed trait PaymentResponse
case object PaymentDone extends PaymentResponse