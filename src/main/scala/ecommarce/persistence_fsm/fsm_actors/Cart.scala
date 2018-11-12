package ecommarce.persistence_fsm.fsm_actors

import ecommarce.persistence_fsm.utils.Item

trait CartData {
  val items: Map[String, Item]
  def addItem(newItem: Item): Cart
  def removeItem(itemToRemove: String, quantity: Int): Cart
}

case class Cart(items: Map[String, Item] = Map.empty) extends CartData {
  def addItem(newItem: Item): Cart = {
    val currentCount = if (items contains newItem.id) items(newItem.id).count else 0
    copy(items = items.updated(newItem.name, newItem.copy(count = currentCount + newItem.count)))
  }

  def removeItem(itemName: String, quantity: Int): Cart = {
    if (items(itemName).count <= quantity) {
      copy(items = items - itemName)
    } else {
      val left = items(itemName).count - quantity
      val updatedItem = Item(itemName, itemName, items(itemName).price, left)
      copy(items = items.updated(itemName, updatedItem))
    }
  }
}

object Cart {
  val empty: Cart = Cart(Map.empty)
}