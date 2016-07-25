package com.freeheap.akkawl.core

import java.util.regex.Pattern

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.message._
import com.freeheap.drawler.dao.LinkSet
import edu.uci.ics.crawler4j.crawler.{CrawlConfig, Page}
import edu.uci.ics.crawler4j.fetcher.{PageFetchResult, PageFetcher}
import edu.uci.ics.crawler4j.parser.Parser
import edu.uci.ics.crawler4j.url.WebURL
import org.apache.http.HttpStatus

import scala.concurrent.duration.FiniteDuration

/**
  * Created by william on 7/6/16.
  */
object Crawler {
  def apply(coord: ActorRef, parserRouter: ActorRef, rConn: String, rSet: String, respectRobot: Boolean) =
    Props(classOf[Crawler], coord, parserRouter, rConn, rSet, respectRobot)
}

class Crawler(coord: ActorRef, parserR: ActorRef, rConn: String, rSet: String, respectRobot: Boolean) extends Actor with ActorLogging {
  // For thread-safe
  val ls = LinkSet(rConn, rSet)
  val FILTERS: Pattern = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g|ico"
    + "|png|tiff?|mid|mp2|mp3|mp4" + "|wav|avi|mov|mpeg|ram|m4v|pdf"
    + "|rm|smil|wmv|swf|wma|zip|rar|gz))$")

  val config: CrawlConfig = new CrawlConfig
  config.setIncludeBinaryContentInCrawling(false)
  config.setPolitenessDelay(1000)
  val parser = new Parser(config)
  val pageFetcher: PageFetcher = new PageFetcher(config)
  val lse = ls.exists(LinkSet.chckExistsFromSingle) _
  val lsa = ls.addSet(LinkSet.addDataToSingle) _

  import context.dispatcher

  val tick =
    context.system.scheduler.schedule(FiniteDuration(0, "millis"), FiniteDuration(20, "millis"), self, PeriodicM)

  override def postStop() = tick.cancel()


  override def receive: Receive = {
    case cu: CrawlingUrl =>
      getUrl(shouldVisit)(cu.url)
      sender ! Finish(cu.url)
    case PeriodicM =>
      // can do some other works
      coord ! NeedMoreMsg
  }

  //TODO: can't use one single queue here to check for visited links as well as unvisited links
  //Need to re-work on this
  private def shouldVisit(url: String): Boolean = {
    val normUrl: String = url.toLowerCase
    if (respectRobot) {
       !FILTERS.matcher(normUrl).matches && !lse(normUrl) && doesRobotAllow(url)
    } else {
      !FILTERS.matcher(normUrl).matches
    }
  }

  // TODO get robot.txt from Redis or directly from website and save to C* for later use
  private[this] def doesRobotAllow(url: String): Boolean = {
    true
  }

  private[this] def getUrl(f: (String) => Boolean)(url: String) = {
    log.debug("Crawling: ", url)
    if (f(url)) {
      val page = downloadPage(url)
      page match {
        case Some(p) =>
          parserR ! CrawledPageData(p, System.currentTimeMillis())
        case None =>
          log.debug(s"Crawling $url not found")
      }
    }
  }

  private def downloadPage(url: String): Option[Page] = {
    val curURL: WebURL = new WebURL
    curURL.setURL(url)
    var fetchResult: PageFetchResult = null
    try {
      fetchResult = pageFetcher.fetchPage(curURL)
      if (fetchResult.getStatusCode == HttpStatus.SC_OK) {
        val page: Page = new Page(curURL)
        fetchResult.fetchContent(page)
        parser.parse(page, curURL.getURL)
        return Some(page)
      } else if (fetchResult.getMovedToUrl != null) {
        lsa(url)
        log.debug(s"Redirect $url to ${fetchResult.getMovedToUrl}")
        return downloadPage(fetchResult.getMovedToUrl)
      } else {
        log.debug(s"Status code for $url: ${fetchResult.getStatusCode}")
      }
    } catch {
      case e: Exception =>
        log.error(s"Error occurred while fetching url: ${curURL.getURL}", e)
    } finally {
      if (fetchResult != null) {
        fetchResult.discardContentIfNotConsumed()
      }
    }
    None
  }
}
