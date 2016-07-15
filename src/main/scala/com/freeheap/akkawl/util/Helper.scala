package com.freeheap.akkawl.util

import java.net.URL

/**
  * Created by william on 7/11/16.
  */
object Helper {
  def urlParser(url: String): (String, String, Boolean) = {
    if (url != null) {
      val u = new URL(url)
      (u.getHost, url, true)
    } else ("", "", false)
  }

}
