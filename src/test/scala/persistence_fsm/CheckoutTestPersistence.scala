package persistence_fsm

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import ecommarce.persistence_fsm.fsm_actors.FSMCheckout
import ecommarce.persistence_fsm.messages._
import ecommarce.persistence_fsm.utils.{PayPal, StringDelivery}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class CheckoutTestPersistence extends TestKit(ActorSystem())
  with WordSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll(): Unit = {
    system.terminate
  }

  "Checkout actor" must {
    "send CheckoutClosed message to Cart" in {
      val cartParent = TestProbe()
      val checkoutChild = cartParent.childActorOf(Props(new FSMCheckout("testCheckout1")))

      checkoutChild ! StartCheckout
      checkoutChild ! SelectDeliveryMethod(StringDelivery("dhl"))
      expectMsg(DeliveryMethodSelected)

      checkoutChild ! SelectPaymentMethod(PayPal)
      expectMsgType[PaymentServiceStarted]

      checkoutChild ! PaymentReceived

      cartParent.expectMsg(CheckoutClosed)
    }

    "checkout expired" in {
      val cartParent = TestProbe()
      val checkoutChild = cartParent.childActorOf(Props(new FSMCheckout("testCheckout2")))

      checkoutChild ! StartCheckout
      Thread sleep 4000

      cartParent.expectMsg(CheckoutCanceled)
    }

    "payment expired" in {
      val cartParent = TestProbe()
      val checkoutChild = cartParent.childActorOf(Props(new FSMCheckout("testCheckout3")))

      checkoutChild ! StartCheckout
      checkoutChild ! SelectDeliveryMethod(StringDelivery("dhl"))
      Thread sleep 4000

      cartParent.expectMsg(CheckoutCanceled)
    }
  }
}

