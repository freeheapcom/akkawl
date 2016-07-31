package com.freeheap.akkawl.core

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.actor.Actor.Receive
import com.freeheap.akkawl.message._
import com.freeheap.akkawl.util.Helper
import com.freeheap.drawler.dao.LinkQueue

import scala.concurrent.duration.FiniteDuration

/**
  * Created by william on 7/6/16.
  */

object Coordinator {
  def apply(rConn: String, rQueue: String, batchSize: Int = 100, periodic: Int = 500) =
    Props(classOf[Coordinator], rConn, rQueue, batchSize, periodic)
}

class Coordinator(rConn: String, rQueue: String, batchSize: Int = 100, periodic: Int = 500) extends Actor with ActorLogging {
  val lq = LinkQueue(rConn, rQueue)
  final val MAX_QUEUE_SIZE = 10000

  val q = new LinkedBlockingQueue[String]()

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

  override def postRestart(reason: Throwable) = {}


  override def receive: Receive = {
    case LogPeriodically =>
      val cur = deliverCounter.get()
      log.info(s"Current queue size: ${q.size()}, deliver rate: ${(cur - oldDeliverCounter.get()) / periodic * 100}")
      oldDeliverCounter.set(cur)
    case PeriodicM =>
      loadDataFromRedis()
    case ForceGetData =>
      loadDataFromRedis()
    case NeedMoreMsg =>
      deliverMsg(sender())
    case Finish(url, domain) =>
      decrRate(domain)
      deliverMsg(sender())
  }

  private val rates = new scala.collection.mutable.HashMap[String, Int]

  private[this] def incrRate(domain: String) = {
    this.synchronized {
      rates(domain) = rates.getOrElseUpdate(domain, 0) + 1
    }
  }

  private[this] def decrRate(domain: String) = {
    this.synchronized {
      rates(domain) = rates.getOrElseUpdate(domain, 1) - 1
    }
  }

  private final val MAX_ALLOWED_REQUEST = 20

  private[this] def deliverMsg(s: ActorRef): Unit = {
    popQueue match {
      case Some(url) =>
        val duo = Helper.getDomainProtocol(url)
        duo match {
          case Some(du) =>
            if (rates.getOrElse(du._1, 0) <= MAX_ALLOWED_REQUEST) {
              deliverCounter.incrementAndGet()
              s ! CrawlingUrl(du._2, du._1, 1)
              incrRate(du._1)
            } else {
              pushQueue(url)
              deliverMsg(s)
            }
          case None =>
        }
      case None => 
    }
  }

  private[this] def popQueue = Option(q.poll())

  private[this] def pushQueue(item: String) = q.offer(item)

  private[this] def loadDataFromRedis() = {
    1.to(batchSize).filter(_ => q.size() < MAX_QUEUE_SIZE).foreach(_ => {
      val i = lq.popQueue(LinkQueue.getDataFromSingle)
      i match {
        case Some(url) => q.add(url)
        case None =>
      }
    })
  }

}
