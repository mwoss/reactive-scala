package persistence_fsm

import java.net.URI

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestFSMRef, TestKit}
import akka.util.Timeout
import ecommarce.persistence_fsm.fsm_actors.FSMOrderManager
import ecommarce.persistence_fsm.fsm_actors.FSMOrderManager._
import ecommarce.persistence_fsm.messages._
import ecommarce.persistence_fsm.utils.{Item, PayPal, StringDelivery}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Milliseconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class OrderManagerTestPersistence
  extends TestKit(ActorSystem("OrderManagerTest"))
    with WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with Matchers {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = Span(50000, Milliseconds))
  implicit val timeout: Timeout = 1.second

  "An order manager" must {
    "supervise whole order process" in {

      def sendMessageAndValidateState(orderManager: TestFSMRef[OrderManagerState, OrderManagerData, FSMOrderManager],
                                       message: OrderManagerCommand,
                                       expectedState: OrderManagerState
                                     ): Unit = {
        (orderManager ? message).mapTo[OrderManagerEvent].futureValue shouldBe Done
        orderManager.stateName shouldBe expectedState
      }
      val orderManager = TestFSMRef[OrderManagerState, OrderManagerData, FSMOrderManager](new FSMOrderManager())
      orderManager.stateName shouldBe Uninitialized

      sendMessageAndValidateState(orderManager, StartShopping, Open)

      sendMessageAndValidateState(orderManager, AddItem(Item(new URI("rollerblades"), "rollerblades", BigDecimal(500), 5)), Open)

      sendMessageAndValidateState(orderManager, Buy, InCheckout)

      sendMessageAndValidateState(orderManager, SelectDeliveryMethod(StringDelivery("inpost")), InCheckout)

      sendMessageAndValidateState(orderManager, SelectPaymentMethod(PayPal), InPayment)

      sendMessageAndValidateState(orderManager, Pay, Finished)
    }
  }
}