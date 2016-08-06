package com.freeheap.akkawl.util

import com.freeheap.akkawl.util.Helper
import edu.uci.ics.crawler4j.crawler.Page
import edu.uci.ics.crawler4j.fetcher.{PageFetchResult, PageFetcher}
import org.scalatest._

/**
  * Created by william on 8/5/16.
  */
class HelperTest extends FlatSpec {
  "Domain validator" should "return true in these cases" in {
    val domains = Array("http://nytimes.com", "https://google.com.sg", "https://engineering.twitter.com")

    val remaining = domains.filter(d => !Helper.isValidDomain(d))
    assert(remaining.isEmpty)
  }
  it should "return false in these" in {
    val domains = Array("httpa://nytimes.com", "google.com.sg", "fap://engineering.twitt", "http://vnexpress.net", "https://tinhte.vn")

    val remaining = domains.filter(d => Helper.isValidDomain(d))
    assert(remaining.size == 2)
  }

  "Url parser" should "return port in the domain field" in {
    val domains = Array("https://freeheap.io:1881/test", "https://google.com.vn:1881/search")
    val number = "[0-9]+".r
    val d = domains.flatMap(u => Helper.getDomainProtocol(u)).map(_._2).filter(number.findFirstIn(_) match {
      case Some(d) => false
      case None => true
    })
    assert(d.size == 2)
  }

  it should "return no port in the domain field" in {
    val domains = Array("http://freeheap.io:80/test", "https://google.com.vn:443/testsjfkds")
    val number = "[0-9]+".r
    val d = domains.flatMap(Helper.getDomainProtocol).map(_._2).filter(number.findFirstIn(_) match {
      case Some(d) => true
      case None => false
    })
    val p: Page  = null
    val fetcherR: PageFetchResult = null
    val fetcher :PageFetcher = null

    p.getWebURL.
    assert(d.isEmpty)
  }
}

