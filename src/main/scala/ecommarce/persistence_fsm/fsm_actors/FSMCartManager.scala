package ecommarce.persistence_fsm.fsm_actors

import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import FSMCartManager._
import akka.actor.Props
import akka.event.Logging
import ecommarce.persistence_fsm.messages._
import ecommarce.persistence_fsm.utils.Item

import scala.reflect._
import scala.concurrent.duration._

class FSMCartManager(cartManagerID: String) extends PersistentFSM[State, CartData, CartDomainEvent]{
  override def persistenceId: String = cartManagerID
  override def domainEventClassTag: ClassTag[CartDomainEvent] = classTag[CartDomainEvent]

  val logger = Logging(context.system, this)
  val timerName = "cartTimer"
  val timeout: FiniteDuration = 3.seconds

  startWith(Empty, Cart.empty)

  when(Empty){
    case Event(AddItem(item), _) =>
      log.info("Added new item to empty cart: " + item.name)
      goto(NonEmpty) applying (AddItemDomainEvent(item), StartCartTimerDomainEvent) replying ItemAdded
  }

  when(NonEmpty){
    case Event(AddItem(item), _) =>
      log.info("Added new item to cart: " + item.name)
      stay applying AddItemDomainEvent(item) replying ItemAdded
    case Event(RemoveItem(itemName, quantity), cartData) =>
      log.info(itemName + " removed")
      val afterRemove: Cart = cartData.removeItem(itemName, quantity)
      if (afterRemove.items.nonEmpty) {
        stay applying RemoveItemDomainEvent(itemName, quantity) replying ItemRemoved
      } else {
        goto(Empty) applying (RemoveItemDomainEvent(itemName, quantity), ResetCartDomainEvent) replying ItemRemoved
      }
    case Event(CheckState, cartData) =>
      logger.info("Item in cart {}", cartData.items)
      stay replying StateChecked(cartData.items)
    case Event(StartCheckout, _) =>
      log.info("Starting checkout")
      val checkoutActor = context.actorOf(Props(new FSMCheckout("checkout")))
      checkoutActor ! StartCheckout
      goto(InCheckout) applying CancelCartTimerDomainEvent replying CheckoutStarted(checkoutActor)
  }

  when(InCheckout){
    case Event(CheckoutCanceled, _) =>
      log.info("Checkout canceled")
      goto(Empty) applying ResetCartDomainEvent
    case Event(CheckoutClosed, _) =>
      log.info("Checkout closed - cart")
      context.parent ! CartEmptied
      cancelTimer(timerName)
      goto(Empty) applying ResetCartDomainEvent
  }

  whenUnhandled {
    case Event(CartTimerExpired, _) =>
      goto(Empty) applying ResetCartDomainEvent
    case Event(e, s) =>
      logger.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  override def applyEvent(domainEvent: CartDomainEvent, currentData: CartData): CartData = {
    domainEvent match {
      case AddItemDomainEvent(item) =>
        currentData.addItem(item)
      case RemoveItemDomainEvent(itemName, quantity) =>
        currentData.removeItem(itemName, quantity)
      case StartCartTimerDomainEvent =>
        setTimer(timerName, CartTimerExpired, timeout)
        currentData
      case CancelCartTimerDomainEvent =>
        cancelTimer(timerName)
        currentData
      case ResetCartDomainEvent =>
        logger.info("Applying reset cart")
        cancelTimer(timerName)
        Cart.empty
    }
  }
}

object FSMCartManager {
  sealed trait State extends FSMState
  case object Empty extends State {
    override def identifier: String = "Empty"
  }
  case object NonEmpty extends State {
    override def identifier: String = "NonEmpty"
  }
  case object InCheckout extends State {
    override def identifier: String = "InCheckout"
  }

  sealed trait CartDomainEvent
  case class AddItemDomainEvent(item: Item) extends CartDomainEvent
  case class RemoveItemDomainEvent(item: String, quantity: Int) extends CartDomainEvent
  case object StartCartTimerDomainEvent extends CartDomainEvent
  case object CancelCartTimerDomainEvent extends CartDomainEvent
  case object ResetCartDomainEvent extends CartDomainEvent
}
