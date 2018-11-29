package persistence_fsm

import ecommarce.persistence_fsm.SearchService
import org.scalatest.{FlatSpec, Matchers}

class SearchServiceTest extends FlatSpec with Matchers {
  "A Search Service" should "load the file from resources" in {
    val searchService = new SearchService()
    searchService.brandItemsMap.size shouldBe 10977
  }
}