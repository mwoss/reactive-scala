package ecommarce.persistence_fsm.utils

case class StringDelivery(delivery: String) extends AnyVal
case class StringPayment(payment: String) extends AnyVal
case class StringItem(item: String) extends AnyVal

case class Item(id: String, name: String, price: BigDecimal, count: Int = 1) {
  override def toString: String = name + " " + price.toString
}