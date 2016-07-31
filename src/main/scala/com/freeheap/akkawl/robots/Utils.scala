package com.freeheap.akkawl.robots

import java.util.StringTokenizer
import java.util.regex.{Matcher, Pattern}

/**
  * Created by william on 7/21/16.
  */
object Utils {
  val pattern = Pattern.compile("\\b(((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)" +
    "(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov" +
    "|mil|biz|info|mobi|name|aero|jobs|museum" +
    "|travel|[a-z]{2}))(:[\\d]{1,5})?" +
    "(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?" +
    "((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" + "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)" +
    "(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?" + "([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*" +
    "(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b")
  val PATTERNS_USERAGENT: String = "(?i)^User-agent:.*"
  val PATTERNS_DISALLOW: String = "(?i)Disallow:.*"
  val PATTERNS_ALLOW: String = "(?i)Allow:.*"
  val PATTERNS_RATE: String = "(?i)Crawl-delay:.*"
  val PATTERNS_USERAGENT_LENGTH: Int = 11
  val PATTERNS_DISALLOW_LENGTH: Int = 9
  val PATTERNS_ALLOW_LENGTH: Int = 6

  def extractUrls(input: String): Set[WebURL] = {
    val extractedUrls: scala.collection.mutable.Set[WebURL] = new scala.collection.mutable.HashSet[WebURL]
    if (input != null) {
      val matcher: Matcher = pattern.matcher(input)
      while (matcher.find) {
        var urlStr: String = matcher.group
        if (!urlStr.startsWith("http"))
          urlStr = "http://" + urlStr
        extractedUrls.add(WebURL(urlStr))
      }
    }

    extractedUrls.toSet
  }

  /**
    * @param content
    * @param uag
    * @return
    */
  def disassemblyRobots(content: String, uag: String): RuleSet = {
    val rs = RuleSet()
    var inMatchingUserAgent: Boolean = false
    val st: StringTokenizer = new StringTokenizer(content, "\n\r")
    while (st.hasMoreTokens) {
      var line: String = st.nextToken
      val commentIndex: Int = line.indexOf('#')
      if (commentIndex > -1)
        line = line.substring(0, commentIndex)
      line = line.replaceAll("<[^>]+>", "")
      line = line.trim
      if (line.nonEmpty) {
        if (line.matches(PATTERNS_USERAGENT)) {
          val ua: String = line.substring(PATTERNS_USERAGENT_LENGTH).trim.toLowerCase
          inMatchingUserAgent = "*" == ua || ua.contains(uag)
        } else if (line.matches(PATTERNS_DISALLOW)) {
          if (inMatchingUserAgent) {
            var path: String = line.substring(PATTERNS_DISALLOW_LENGTH).trim
            if (path.endsWith("*")) {
              path = path.substring(0, path.length - 1)
            }
            path = path.trim
            if (!path.isEmpty) {
              rs.add(path)
            }
          }
        } else if (line.matches(PATTERNS_ALLOW)) {
        }
      }
    }
    rs
  }

}
