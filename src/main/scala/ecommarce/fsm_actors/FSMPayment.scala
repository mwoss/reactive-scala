package ecommarce.fsm_actors

import akka.actor.FSM
import FSMPayment._
import akka.event.Logging
import ecommarce.messages._


class FSMPayment extends FSM[State, Data]{
  val logger = Logging(context.system, this)

  startWith(Uninitialized, NoneData)

  when(Uninitialized) {
    case Event(StartPayment, NoneData) =>
      logger.info("Initialize payment")
      goto(OpenPayment) using NoneData
  }

  when(OpenPayment) {
    case Event(Pay, NoneData) =>
      logger.info("$$$ Money money $$$")
      sender() ! PaymentConfirmed
      context.parent ! PaymentReceived
      context stop self
      stay()
  }

  whenUnhandled {
    case Event(e, s) =>
      logger.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}
object FSMPayment{
  sealed trait State
  case object Uninitialized extends State
  case object OpenPayment extends State

  sealed trait Data
  case object NoneData extends Data
}