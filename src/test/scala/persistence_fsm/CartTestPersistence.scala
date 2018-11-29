package persistence_fsm

import java.net.URI

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import ecommarce.persistence_fsm.fsm_actors.FSMCartManager
import ecommarce.persistence_fsm.messages._
import ecommarce.persistence_fsm.utils.Item
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CartTestPersistence extends TestKit(ActorSystem("CartTest"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ScalaFutures
  with Matchers {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(10000, Milliseconds))

  override def afterAll(): Unit = {
    system.terminate()
  }

  "CartActor" must {
    "contains item after addition" in {
      val cartActor = system.actorOf(Props(new FSMCartManager("testCartManager1")))

      cartActor ! AddItem(Item(new URI("pencil"), "pencil", BigDecimal(2), 2))

      expectMsg(ItemAdded)
    }

    "contains 1 item after removal" in {
      val cartActor = system.actorOf(Props(new FSMCartManager("testCartManager2")))
      val toRemove = new URI("pencil")
      cartActor ! AddItem(Item(toRemove, "pencil", BigDecimal(2), 2))
      expectMsg(ItemAdded)
      cartActor ! AddItem(Item(new URI("notebook"), "notebook", BigDecimal(5000), 2))
      expectMsg(ItemAdded)
      cartActor ! RemoveItem(toRemove, 2)
      expectMsg(ItemRemoved)
    }

    "contains no item after timer expiration" in {
      val cartActor = system.actorOf(Props(new FSMCartManager("testCartManager3")))

      cartActor ! AddItem(Item(new URI("pencil"), "pencil", BigDecimal(2), 2))
      expectMsg(ItemAdded)
      cartActor ! AddItem(Item(new URI("notebook"), "notebook", BigDecimal(5000), 2))
      expectMsg(ItemAdded)

      Thread sleep 3500

      cartActor ! CheckState
      expectMsg(StateChecked(Map.empty))
    }

    "cart should be in checkout" in {
      val cartActor = system.actorOf(Props(new FSMCartManager("testCartManager4")))

      cartActor ! AddItem(Item(new URI("notebook"), "notebook", BigDecimal(5000), 2))
      cartActor ! StartCheckout

      expectMsgType[CheckoutStarted]
    }
  }
}
