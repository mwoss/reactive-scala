import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestFSMRef, TestKit}
import akka.util.Timeout
import ecommarce.pure_fsm.fsm_actors.FSMOrderManager
import ecommarce.pure_fsm.fsm_actors.FSMOrderManager._
import ecommarce.pure_fsm.messages._
import ecommarce.pure_fsm.utils.{StringDelivery, StringItem, StringPayment}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class OrderManagerTest
  extends TestKit(ActorSystem("OrderManagerTest"))
    with WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with Matchers {

  implicit val timeout: Timeout = 1.second

  "An order manager" must {
    "supervise whole order process" in {

      def sendMessageAndValidateState(
                                       orderManager: TestFSMRef[OrderManagerState, OrderManagerData, FSMOrderManager],
                                       message: OrderManagerCommand,
                                       expectedState: OrderManagerState
                                     ): Unit = {
        (orderManager ? message).mapTo[OrderManagerEvent].futureValue shouldBe Done
        orderManager.stateName shouldBe expectedState
      }

      val orderManager = TestFSMRef[OrderManagerState, OrderManagerData, FSMOrderManager](new FSMOrderManager())
      orderManager.stateName shouldBe Uninitialized

      sendMessageAndValidateState(orderManager, StartShopping, Open)

      sendMessageAndValidateState(orderManager, AddItem(StringItem("rollerblades")), Open)

      sendMessageAndValidateState(orderManager, Buy, InCheckout)

      sendMessageAndValidateState(orderManager, SelectDeliveryMethod(StringDelivery("inpost")), InCheckout)

      sendMessageAndValidateState(orderManager, SelectPaymentMethod(StringPayment("paypal")), InPayment)

      sendMessageAndValidateState(orderManager, Pay, Finished)
    }
  }
}