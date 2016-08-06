package com.freeheap.akkawl.core


import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import java.util.concurrent.LinkedBlockingQueue

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.message._
import com.freeheap.akkawl.util.{Helper, RateLimiter}
import com.freeheap.drawler.dao.RedisQueue

import scala.concurrent.duration.FiniteDuration

/**
  * Created by william on 7/6/16.
  *
  **/
object Coordinator {
  def apply(rConn: String, rQueue: String, batchSize: Int = 100, periodic: Int = 500) =
    Props(classOf[Coordinator], rConn, rQueue, batchSize, periodic)
}

class Coordinator(rConn: String, rQueue: String, batchSize: Int = 100, periodic: Int = 500)
  extends Actor with ActorLogging {
  final val MAX_QUEUE_SIZE = 10000

  val redisQueue = RedisQueue(rConn, rQueue)
  val linkedBlockingQueue = new LinkedBlockingQueue[String]()

  // TODO add some counters
  val deliverCounter = new AtomicLong(0)
  val oldDeliverCounter = new AtomicLong(0)

  import context.dispatcher

  val tick =
    context.system.scheduler.schedule(FiniteDuration(0, "millis"), FiniteDuration(periodic, "millis"), self, PeriodicM)
  val logTick =
    context.system.scheduler.schedule(FiniteDuration(0, "millis"), FiniteDuration(periodic * 10, "millis"), self, LogPeriodically)

  override def postStop() = {
    tick.cancel()
    logTick.cancel()
  }

  override def preStart() = {}

  override def postRestart(reason: Throwable) = {
    log.info(s"Restarted from ${reason.getMessage}", reason)
  }

  override def receive: Receive = {
    case LogPeriodically =>
      val cur = deliverCounter.get()
      log.info(s"Current queue size: ${linkedBlockingQueue.size()}, deliver rate: ${(cur - oldDeliverCounter.get()) / periodic * 1000}")
      oldDeliverCounter.set(cur)
    case PeriodicM =>
      log.debug("Periodically get new urls")
      loadDataFromRedis()
    case ForceGetData =>
      log.debug("Force getting new urls")
      loadDataFromRedis()
    case NeedMoreMsg =>
      log.debug(s"${sender().path} asks for new message")
      deliverMsg(sender())
    case FinishCrawling(domain, url) =>
      RateLimiter.returnPermit(domain)
      RateLimiter.printCounter(domain, url)
      deliverMsg(sender())
  }

  private[this] def deliverMsg(crawler: ActorRef): Unit = {
    popQueue match {
      case Some(url) =>
        val duo = Helper.getDomainProtocol(url)
        duo match {
          case Some(du) =>
            if (RateLimiter.tryAcquire(du._2)) {
              deliverCounter.incrementAndGet()
              crawler ! CrawlingUrl(du._1, du._2, du._3, 1)
            } else {
              //return permit
              RateLimiter.printCounter(du._2, du._3)
              pushQueue(url)
              //deliverMsg(crawler)
            }
          case None => //self ! ForceGetData
        }
      case None =>
    }
  }

  private[this] def popQueue = Option(linkedBlockingQueue.poll())

  private[this] def pushQueue(item: String) = linkedBlockingQueue.offer(item)

  private[this] def loadDataFromRedis() = {
    log.debug("Loading data")
    val ai = new AtomicInteger()
    1.to(batchSize).filter(_ => linkedBlockingQueue.size() < MAX_QUEUE_SIZE).foreach(_ => {
      val i = redisQueue.popQueue(RedisQueue.getDataFromSingle)
      i match {
        case Some(url) =>
          ai.incrementAndGet()
          linkedBlockingQueue.add(url)

        case None =>
      }
    })
    log.debug(s"Loading ${ai.get()} url(s)....done")
  }

}
