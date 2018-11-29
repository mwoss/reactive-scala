package ecommarce.persistence_fsm.utils

import java.net.URI

case class StringDelivery(delivery: String) extends AnyVal
case class StringPayment(payment: String) extends AnyVal
case class StringItem(item: String) extends AnyVal

case class Item(id: URI, name: String, price: BigDecimal, count: Int = 1) {
  override def toString: String = name + " " + price.toString
}

trait PaymentMethodType
case object PayPal extends PaymentMethodType
case object Blik extends PaymentMethodType
case object Cash extends PaymentMethodType