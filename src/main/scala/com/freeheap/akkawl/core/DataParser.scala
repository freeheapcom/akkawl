package com.freeheap.akkawl.core

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.message.{CrawledPageData, StorageData}
import edu.uci.ics.crawler4j.parser.{HtmlParseData, ParseData}

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
    val parseData: ParseData = cpd.p.getParseData
    if (parseData != null) {
      parseData match {
        case hpd: HtmlParseData =>
          import scala.collection.JavaConversions._
          val (url, domain, content, outlink) = (cpd.p.getWebURL.getURL, cpd.p.getWebURL.getDomain, hpd.getText, hpd.getOutgoingUrls.map(_.getURL).toSet)
          loader ! StorageData(url, domain, content, cpd.ts, outlink)
        case _ =>
          log.debug(s"Cannot parse html from data $cpd")
      }
    } else {
      log.debug("Couldn't parse the content of the page.")
    }
  }
}
