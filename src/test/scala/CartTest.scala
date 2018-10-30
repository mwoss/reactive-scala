import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import ecommarce.fsm_actors.FSMCart
import ecommarce.fsm_actors.FSMCart._
import ecommarce.messages.{AddItem, RemoveItem, StartCheckout}
import ecommarce.utils.StringItem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class CartTest extends TestKit(ActorSystem("CartTest"))
  with WordSpecLike
  with BeforeAndAfterAll
  with ScalaFutures
  with Matchers{

  override def afterAll(): Unit = {
    system.terminate()
  }

  "CartActor" must {
    "contains item after addition" in {
      val cartActor = TestFSMRef(new FSMCart)

      cartActor ! AddItem(StringItem("pencil"))

      cartActor.stateName shouldBe NonEmpty
      cartActor.stateData shouldBe NonEmptyCart(Set(StringItem("pencil")))
    }

    "contains 1 item after removal" in {
      val cartActor = TestFSMRef(new FSMCart)

      cartActor ! AddItem(StringItem("pencil"))
      cartActor ! AddItem(StringItem("notebook"))
      cartActor ! RemoveItem(StringItem("pencil"))

      cartActor.stateName shouldBe NonEmpty
      cartActor.stateData shouldBe NonEmptyCart(Set(StringItem("notebook")))
    }

    "contains no item after timer expiration" in {
      val cartActor = TestFSMRef(new FSMCart)

      cartActor ! AddItem(StringItem("pencil"))
      cartActor ! AddItem(StringItem("notebook"))

      Thread sleep 3500

      cartActor.stateName shouldBe Empty
      cartActor.stateData shouldBe EmptyCart
    }

    "cart should be in checkout" in {
      val cartActor = TestFSMRef(new FSMCart)

      cartActor ! AddItem(StringItem("pencil"))
      cartActor ! StartCheckout

      cartActor.stateName shouldBe InCheckout
      cartActor.stateData shouldBe NonEmptyCart(Set(StringItem("pencil")))
    }
  }

}
