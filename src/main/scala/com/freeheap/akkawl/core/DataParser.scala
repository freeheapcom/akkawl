package com.freeheap.akkawl.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.message.{CrawledPageData, StorageData}
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
      val outLinks = extractOutLinks(cpd.domain, content)
      parsedData match {
        case Some(mainContent) =>
          loader ! StorageData(cpd.url, cpd.domain, mainContent, cpd.ts, outLinks)
        case None =>
          log.debug(s"Cannot parse html from data $cpd")
      }
    } else {
      log.debug("Couldn't parse the content of the page.")
    }
  }

  private[this] def extractOutLinks(domain: String, content: String): Set[String] = {
    val doc = Jsoup.parse(content)
    val links = doc.select("a[href]")
    import scala.collection.JavaConversions._
    links.map(link => {
      val l = link.attr("href").trim
      if (l.startsWith("/")) s"$domain/$l"
      else l
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
