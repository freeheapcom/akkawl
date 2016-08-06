package com.freeheap.akkawl.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.message.{CrawledPageData, StorageData}
import com.freeheap.akkawl.util.Helper
import com.kohlschutter.boilerpipe.extractors.ArticleExtractor
import edu.uci.ics.crawler4j.parser.{HtmlParseData, ParseData}
import org.jsoup.Jsoup

/**
  * Created by william on 7/6/16.
  */
object DataParser {
  def apply(loader: ActorRef) = Props(classOf[DataParser], loader)
}

class DataParser(loader: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case cpd: CrawledPageData =>
      parseData(cpd)
  }

  private[this] def parseData(cpd: CrawledPageData): Unit = {
    val content: String = cpd.content
    if (content != null) {
      val parsedData = extractMainContent(content)
      val outLinks = extractOutLinks(cpd.protocol, cpd.domain, cpd.url, content)
      parsedData match {
        case Some(mainContent) =>
          loader ! StorageData(cpd.protocol, cpd.url, cpd.domain, mainContent, cpd.ts, outLinks)
        case None =>
          log.debug(s"Cannot parse html from data $cpd")
      }
    } else {
      log.debug("Couldn't parse the content of the page.")
    }
  }

  private[this] def extractOutLinks(protocol: String, domain: String, url: String, content: String): Set[String] = {
    val doc = Jsoup.parse(content)
    val links = doc.select("a[href]")
    val p = "^\\w+\\:.*".r
    import scala.collection.JavaConversions._
    links.flatMap(link => {
      val l = link.attr("href").trim
      if (!(l == null || l.isEmpty)) {
        val validLink = if (p.findFirstIn(l).isDefined) l
        else if (url.charAt(url.length - 1) == '/' || l.charAt(0) == '/' || url.endsWith("%2F") || l.startsWith("%2F") ||
          url.charAt(url.length - 1) == '#' || l.charAt(0) == '#' || url.endsWith("%23") || l.startsWith("%23"))
          s"$url$l"
        else s"$url/$l"
        if (Helper.isValidDomain(validLink)) {
          Some(validLink)
        } else None
      } else None
    }).toSet
  }

  val extractor = ArticleExtractor.INSTANCE

  private[this] def extractMainContent(content: String): Option[String] = {
    try {
      Some(extractor.getText(content))
    } catch {
      case e: Throwable =>
        log.error(s"Cannot extract main content of $content", e)
        None
    }
  }
}
