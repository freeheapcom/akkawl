package com.freeheap.akkawl.util

import java.net.URL

import com.freeheap.akkawl.robots.WebURL
import org.apache.commons.validator.routines.UrlValidator

/**
  * Created by william on 7/11/16.
  */
object Helper {

  @deprecated
  def urlParser(url: String): (String, String, Boolean) = {
    if (url != null) {
      val u = new URL(url)
      (u.getHost, url, true)
    } else ("", "", false)
  }

  def getDomain(url: String): Option[(String, String)] = {
    Option(if (url != null) {
      val u = WebURL(url)
      (u.domain, url)
    } else null)
  }

  def getDomainProtocol(url: String): Option[(String, String)] = {
    Option(if (url != null) {
      val u = WebURL(url)
      (s"http://${u.domain}", url)
    } else null)
  }

  val schemes = Array("http", "https")
  val urlValidator = new UrlValidator(schemes)

  def isValidDomain(domain: String): Boolean = {
    urlValidator.isValid(domain)
  }

}
