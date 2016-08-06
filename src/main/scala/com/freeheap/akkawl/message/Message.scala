package com.freeheap.akkawl.message

/**
  * Created by william on 7/15/16.
  */
case class Message()

case class Url(url: String)

case class CrawlingUrl(protocol: String, domain: String, url: String, simulConn: Int)

case class CrawledData(protocol: String, url: String, domain: String, content: String, ts: Long)

case class CrawledPageData(protocol: String, domain: String, url: String, content: String, ts: Long)

case class StorageData(protocol: String, url: String, domain: String, content: String, ts: Long, outLink: Set[String] = Set())

case class FinishCrawling(domain: String, url: String)

case class PeriodicM()

case class NeedMoreMsg()

// coordinator dedicated message types
case class CStartup()

case class LogPeriodically()

case class ForceGetData()