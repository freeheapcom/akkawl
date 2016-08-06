package com.freeheap.akkawl.core

import akka.actor.{Actor, ActorLogging, Props}
import com.freeheap.akkawl.message.StorageData
import com.freeheap.akkawl.util.Logging
import com.freeheap.drawler.common.CrawledDataFullInfo
import com.freeheap.drawler.dao.{CrawlerDataStorage, RedisQueue, RedisSet}

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

  val funcCheckProcessed = RedisSet(rConn, rSet).exists(RedisSet.existsFromSingle) _
  val funcAddToProcessedSet = RedisSet(rConn, rSet).addSet(RedisSet.addDataToSingle) _
  val funcAddToUnprocessedQueue = RedisQueue(rConn, rQueue).pushQueue(RedisQueue.pushDataToSingle) _

  override def receive: Receive = {
    case sd: StorageData =>
      persistData(sd)
      addNewLink(sd)
  }

  private[this] def persistData(sd: StorageData): Unit = {
    log.debug(s"Persisting data ${sd.url}")
    funcAddToProcessedSet(sd.url)
    val fi = CrawledDataFullInfo(sd.domain, sd.url, sd.content, sd.ts, sd.outLink)
    se.saveData(fi)
  }

  private[this] def addNewLink(sd: StorageData) = {
    sd.outLink.foreach(i => {
      if (!funcCheckProcessed(i)) {
        log.debug(s"New link found: $i")
        funcAddToUnprocessedQueue(i)
      }
    })
  }

}
