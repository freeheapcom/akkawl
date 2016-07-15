package com.freeheap.akkawl.core

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}

import akka.actor.{Actor, ActorLogging, Props}
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
      deliverMsg
    case Finish(url) =>
      deliverMsg
  }

  private[this] def deliverMsg = {
    popQueue match {
      case Some(url) =>
        val (domain, u, r) = Helper.urlParser(url)
        if (r) sender ! CrawlingUrl(u, domain, 1)
        else self ! ForceGetData
        deliverCounter.incrementAndGet()
      case None =>
    }
  }

  private[this] def popQueue = Option(q.poll())

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