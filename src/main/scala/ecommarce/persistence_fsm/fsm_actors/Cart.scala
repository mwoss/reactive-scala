package ecommarce.persistence_fsm.fsm_actors

import java.net.URI

import ecommarce.persistence_fsm.utils.Item

trait CartData {
  val items: Map[URI, Item]
  def addItem(newItem: Item): Cart
  def removeItem(itemToRemove: URI, quantity: Int): Cart
}

case class Cart(items: Map[URI, Item] = Map.empty) extends CartData {
  def addItem(newItem: Item): Cart = {
    val currentCount = if (items contains newItem.id) items(newItem.id).count else 0
    copy(items = items.updated(newItem.id, newItem.copy(count = currentCount + newItem.count)))
  }

  def removeItem(itemID: URI, quantity: Int): Cart = {
    if (items(itemID).count <= quantity) {
      copy(items = items - itemID)
    } else {
      val left = items(itemID).count - quantity
      val updatedItem = Item(itemID, items(itemID).name, items(itemID).price, left)
      copy(items = items.updated(itemID, updatedItem))
    }
  }
}

object Cart {
  val empty: Cart = Cart(Map.empty)
}