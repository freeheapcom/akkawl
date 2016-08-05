package com.freeheap.akkawl.core

import java.util.regex.Pattern

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.downloader.Downloader
import com.freeheap.akkawl.downloader.Downloader._
import com.freeheap.akkawl.message._
import com.freeheap.akkawl.robots.RobotsFactory
import com.freeheap.akkawl.util.Helper
import com.freeheap.drawler.dao.{RedisSet, RobotsHash}
import org.apache.http.client.protocol.HttpClientContext

import scala.concurrent.duration.FiniteDuration

/**
  * Created by william on 7/6/16.
  */
object Crawler {
  def apply(coord: ActorRef, parserRouter: ActorRef, rConn: String, rSet: String, rHash: String) =
    Props(classOf[Crawler], coord, parserRouter, rConn, rSet, rHash)
}

class Crawler(coord: ActorRef, parserR: ActorRef, rConn: String, rSet: String, rHash: String)
  extends Actor with ActorLogging {
  val downloader = Downloader.download(Downloader.newClient(), HttpClientContext.create()) _
  // For thread-safety
  val robotsHash = RobotsHash(rConn, rHash)

  val checker = RobotsFactory.newRobotsChecker(robotsHash, RobotsHash.getDataFromSingle, RobotsHash.addDataToSingle)

  val FILTERS: Pattern = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g"
    + "|png|tiff?|mid|mp2|mp3|mp4" + "|wav|avi|mov|mpeg|ram|m4v|pdf"
    + "|rm|smil|wmv|swf|wma|zip|rar|gz))$")

  val funcCheckProcessed = RedisSet(rConn, rSet).exists(RedisSet.existsFromSingle) _
  val funcAddToProcessedSet = RedisSet(rConn, rSet).addSet(RedisSet.addDataToSingle) _

  import context.dispatcher

  val tick =
    context.system.scheduler.schedule(FiniteDuration(0, "millis"), FiniteDuration(50, "millis"), self, PeriodicM)

  override def postStop() = tick.cancel()

  def checkBeforeGet = getUrl(shouldVisit) _

  override def receive: Receive = {
    case cu: CrawlingUrl =>
      println("Crawler processing: url: " + cu.url + ", domain : " + cu.domain)
      try {
        checkBeforeGet(cu.domain, cu.url)
      } catch {
        case e: Throwable =>
        error(s"checkBeforeGet in crawler has error for $cu.url", e)
      }
      sender ! Finish(cu.url, cu.domain)
    case PeriodicM =>
      // can do some other works
      coord ! NeedMoreMsg
  }

  private def shouldVisit(domain: String, url: String): Boolean = {
    val normUrl: String = url.toLowerCase
    val validDomain = isValidDomain(domain)
    val passFilters = !FILTERS.matcher(normUrl).matches
    val isExisted = funcCheckProcessed(normUrl)
    val robotAllows = doesRobotAllow(domain, url)
    validDomain && passFilters && !isExisted && robotAllows
  }

  private def isValidDomain(domain: String): Boolean = {
    Helper.isValidDomain(domain)
  }

  private[this] def doesRobotAllow(domain: String, url: String): Boolean = {
    checker.canCrawl(domain, url)
  }

  private[this] def getUrl(check: (String, String) => Boolean)(domain: String, url: String) = {
    try {
      log.debug("Crawling: ", url)
      if (check(domain, url)) {
        funcAddToProcessedSet(url)
        val page = downloadPage(url)
        page match {
          case Some(p) =>
            parserR ! CrawledPageData(domain, url, p, System.currentTimeMillis())
          case None =>
            log.debug(s"Crawling $url not found") //TODO: should we ignore this page or put into the processed set
        }
      }
    } catch {
       case e: Throwable =>
       error(s"Cannot crawl from $url", e)
    }
  }

  private def downloadPage(url: String) = downloader(url)

}
