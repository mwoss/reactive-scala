package persistence_fsm

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import ecommarce.persistence_fsm.ProductCatalog.GetItems
import ecommarce.persistence_fsm.{ProductCatalog, SearchService}
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._

class ProductCatalogRemoteTest extends AsyncFlatSpec with Matchers {
  implicit val timeout: Timeout = 3.second
  "A remote Product Catalog" should "return search results" in {
    val config = ConfigFactory.load()
    val query = GetItems("gerber", List("cream"))
    val actorSystem = ActorSystem("ProductCatalog", config.getConfig("productcatalog").withFallback(config))
    actorSystem.actorOf(ProductCatalog.props(new SearchService()), "productcatalog")
    val anotherActorSystem = ActorSystem("another")
    val productCatalog =
      anotherActorSystem.actorSelection("akka.tcp://ProductCatalog@127.0.0.1:2554/user/productcatalog")
    for {
      productCatalogActorRef <- productCatalog.resolveOne()
      items <- (productCatalogActorRef ? query).mapTo[ProductCatalog.Items]
      _ <- actorSystem.terminate()
      _ <- anotherActorSystem.terminate()
    } yield {
      assert(items.items.size == 10)
    }
  }
}