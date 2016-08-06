package com.freeheap.akkawl.core

import java.util.regex.Pattern

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.downloader.Downloader
import com.freeheap.akkawl.message._
import com.freeheap.akkawl.robots.RobotsFactory
import com.freeheap.akkawl.util.Helper
import com.freeheap.drawler.dao.{LinkSet, RobotsHash}
import edu.uci.ics.crawler4j.crawler.{CrawlConfig, Page}
import edu.uci.ics.crawler4j.fetcher.PageFetcher
import edu.uci.ics.crawler4j.parser.Parser
import org.apache.commons.validator.routines.UrlValidator
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
  val ls = LinkSet(rConn, rSet)
  val rh = RobotsHash(rConn, rHash)

  val checker = RobotsFactory.newRobotsChecker(rh, RobotsHash.getDataFromSingle, RobotsHash.addDataToSingle)

  val FILTERS: Pattern = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g"
    + "|png|tiff?|mid|mp2|mp3|mp4" + "|wav|avi|mov|mpeg|ram|m4v|pdf"
    + "|rm|smil|wmv|swf|wma|zip|rar|gz))$")

  val checkVisited = ls.exists(LinkSet.chckExistsFromSingle) _
  val markVisited = ls.addSet(LinkSet.addDataToSingle) _

  import context.dispatcher

  val tick =
    context.system.scheduler.schedule(FiniteDuration(0, "millis"), FiniteDuration(50, "millis"), self, PeriodicM)

  override def postStop() = tick.cancel()

  def checkBeforeGet = getUrl(shouldVisit) _

  override def receive: Receive = {
    case cu: CrawlingUrl =>
      checkBeforeGet(cu.protocol, cu.domain, cu.url)
      sender ! FinishCrawling(cu.url, cu.domain)
    case PeriodicM =>
      // can do some other works
      coord ! NeedMoreMsg
  }

  private def shouldVisit(domain: String, url: String): Boolean = {
    val normUrl: String = url.toLowerCase
    val domainCheck = isValidDomain(domain)
    val typeCheck = !FILTERS.matcher(normUrl).matches
    val visited = !checkVisited(normUrl)
    val robotsCheck = doesRobotAllow(domain, url)
    val finalCheck = domainCheck && visited && typeCheck && robotsCheck
    if (finalCheck) log.debug(s"$url is allowed")
    else log.warning(s"check info: domain: $domainCheck visited: $visited type: $typeCheck robots: $robotsCheck")
    finalCheck
  }

  private def isValidDomain(domain: String): Boolean = {
    Helper.isValidDomain(domain)
  }

  private[this] def doesRobotAllow(domain: String, url: String): Boolean = {
    checker.canCrawl(domain, url)
  }

  private[this] def getUrl(check: (String, String) => Boolean)(protocol: String, domain: String, url: String) = {
    log.debug(s"Crawling: $url")
    if (check(domain, url)) {
      markVisited(url)
      val page = downloadPage(domain, url)
      page match {
        case Some(p) =>
          parserR ! CrawledPageData(protocol, domain, url, p, System.currentTimeMillis())
        case None =>
          log.debug(s"Crawler: $url not found")
      }
    } else
      log.warning(s"$url is rejected by checker")
  }

  private def downloadPage(domain: String, url: String) = downloader(domain, url)

}
