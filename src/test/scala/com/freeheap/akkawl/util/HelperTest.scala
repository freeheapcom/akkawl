package com.freeheap.akkawl.util

import com.freeheap.akkawl.util.Helper
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
}
