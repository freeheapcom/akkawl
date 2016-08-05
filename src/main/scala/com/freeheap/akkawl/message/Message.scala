package com.freeheap.akkawl.message

/**
  * Created by william on 7/15/16.
  */
case class Message()

case class Url(url: String)

case class CrawlingUrl(url: String, domain: String, simulConn: Int)

case class CrawledData(url: String, domain: String, content: String, ts: Long)

case class CrawledPageData(domain: String, url: String, content: String, ts: Long)

case class StorageData(url: String, domain: String, content: String, ts: Long, outlink: Set[String] = Set())

case class Finish(url: String, domain: String)

case class PeriodicM()

case class NeedMoreMsg()

// coordinator dedicated message types
case class CStartup()

case class LogPeriodically()

case class ForceGetData()