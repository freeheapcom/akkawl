package com.freeheap.akkawl.robots

import com.freeheap.akkawl.downloader.Downloader
import com.freeheap.drawler.dao.RobotsHash
import com.freeheap.drawler.drivers.RedisConnection

import scala.collection.mutable

/**
  * Created by william on 7/21/16.
  */
class Checker(rs: RobotsHash, rf: (RedisConnection, String, String) => Option[String],
              rfa: (RedisConnection, String, String, String) => Unit) extends Robots {
  val cache = new mutable.HashMap[String, RuleSet]
  val rsrf = rs.getData(rf) _
  val rsrfa = rs.addSet(rfa) _

  /**
    * Download robots or get from cache
    *
    * @param domain
    * @return
    */
  override def getRobots(domain: String): String = {
    Downloader.downloadRobots(domain).getOrElse("")
  }

  /**
    * Parse and produce patterns which servers can block crawlers
    *
    * @param content
    * @return
    */
  override def learnRobots(content: String): RuleSet = {
    RuleSet(content)
  }

  /**
    * Clear local cache
    */
  override def clearCache(): Unit = cache.clear()

  /**
    * Check if we can crawl the specific url
    *
    * @param url
    * @return
    */
  override def canCrawl(url: String): Boolean = {
    val u = WebURL(url)
    canCrawl(u.domain, u.path)
  }


  /**
    * Check if we can crawl the specific url without parsing domain from url
    *
    * @param domain
    * @param path
    * @return
    */
  override def canCrawl(domain: String, path: String): Boolean = {
    val rs: RuleSet = getOrDownload(domain)
    !rs.containsPrefixOf(path)
  }

  private[this] def getOrDownload(domain: String): RuleSet = {
    cache.getOrElse(domain, getFromDbOrDownload(domain))
  }

  private[this] def getFromDbOrDownload(domain: String): RuleSet = {
    learnRobots(rsrf(domain).getOrElse({
      val content = Downloader.downloadRobots(domain).getOrElse("")
      rsrfa(domain, content)
      content
    }))
  }
}
