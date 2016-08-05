package com.freeheap.akkawl.core

import akka.actor.{Actor, ActorLogging, Props}
import com.freeheap.akkawl.message.StorageData
import com.freeheap.akkawl.util.Logging
import com.freeheap.drawler.common.CrawledDataFullInfo
import com.freeheap.drawler.dao.{CrawlerDataStorage, LinkQueue, LinkSet}

/**
  * Created by william on 7/6/16.
  * For loading data to storage and queue
  */
object Loader extends Logging {
  def apply(rConn: String, rQueue: String, rSet: String, se: CrawlerDataStorage) =
    Props(classOf[Loader], rConn, rQueue, rSet, se)
}

class Loader(rConn: String, rQueue: String, rSet: String, se: CrawlerDataStorage)
  extends Actor with ActorLogging {
  val lq = LinkQueue(rConn, rQueue)
  val ls = LinkSet(rConn, rSet)

  val lse = ls.exists(LinkSet.chckExistsFromSingle) _
  val lsa = ls.addSet(LinkSet.addDataToSingle) _
  val lqp = lq.pushQueue(LinkQueue.pushDataToSingle) _

  override def receive: Receive = {
    case sd: StorageData =>
      persistData(sd)
      addNewLink(sd)
  }

  private[this] def persistData(sd: StorageData): Unit = {
    log.debug(s"Persisting data ${sd.url}")
    lsa(sd.url)
    val fi = CrawledDataFullInfo(sd.domain, sd.url, sd.content, sd.ts, sd.outLink)
    se.saveData(fi)
  }

  private[this] def addNewLink(sd: StorageData) = {
    sd.outLink.foreach(i => {
      if (!lse(i)) {
        log.debug(s"New link found: $i")
        lqp(i)
      }
    })
  }

}
