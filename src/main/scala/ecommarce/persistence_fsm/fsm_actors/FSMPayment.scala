package ecommarce.persistence_fsm.fsm_actors

import java.util.UUID

import akka.actor.{ActorRef, FSM, OneForOneStrategy, Props, SupervisorStrategy}
import FSMPayment._
import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.event.Logging
import akka.http.scaladsl.model.{IllegalResponseException, IllegalUriException}
import ecommarce.persistence_fsm.OnlinePaymentStatus
import ecommarce.persistence_fsm.messages._
import ecommarce.persistence_fsm.utils.PaymentMethodType

import scala.concurrent.duration._

class FSMPayment extends FSM[State, Data] {
  val logger = Logging(context.system, this)

  startWith(Uninitialized, NoneData)

  when(Uninitialized) {
    case Event(StartPayment(method), NoneData) =>
      logger.info("Initialize payment")
      goto(OpenPayment) using PaymentMethod(method)
  }

  when(OpenPayment) {
    case Event(Pay, PaymentMethod(method)) =>
      logger.info("$$$ Money money $$$")
      val paymentWorker = context.actorOf(Props(new PaymentWorker()), UUID.randomUUID().toString)
      paymentWorker ! ProceedPayment(method)
      stay() using PaymentMethodWithCheckout(method, sender())
    case Event(OnlinePaymentStatus(status, balance), PaymentMethodWithCheckout(_, checkout)) =>
      logger.info(s"Status: $status, balance $balance")
      checkout ! PaymentConfirmed
      context.parent ! PaymentReceived
      context stop self
      stay()
  }

  whenUnhandled {
    case Event(e, s) =>
      logger.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 10.seconds) {
      case ex@IllegalResponseException(error) =>
        print(error)
        Restart
      case ex@IllegalUriException(error) =>
        println(s"$error")
        Stop
      case ex@spray.json.DeserializationException(error, _, _) =>
        println(error)
        Restart
      case _ =>
        println("Unknown error. Service stopped.")
        Stop
    }

  initialize()
}

object FSMPayment {
  sealed trait State
  case object Uninitialized extends State
  case object OpenPayment extends State

  sealed trait Data
  case object NoneData extends Data
  case class PaymentMethod(method: PaymentMethodType) extends Data
  case class PaymentMethodWithCheckout(method: PaymentMethodType, checkout: ActorRef) extends Data

}