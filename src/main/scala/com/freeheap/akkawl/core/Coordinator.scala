package com.freeheap.akkawl.core

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.freeheap.akkawl.message._
import com.freeheap.akkawl.util.{Helper, RateLimiter}
import com.freeheap.drawler.dao.RedisQueue

import scala.concurrent.duration.FiniteDuration

/**
  * Created by william on 7/6/16.
  */

object Coordinator {
  def apply(rConn: String, rQueue: String, batchSize: Int = 100, periodic: Int = 500) =
    Props(classOf[Coordinator], rConn, rQueue, batchSize, periodic)
}

class Coordinator(rConn: String, rQueue: String, batchSize: Int = 100, periodic: Int = 500) extends Actor with ActorLogging {

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

  override def postRestart(reason: Throwable) = {}


  override def receive: Receive = {
    case LogPeriodically =>
      val cur = deliverCounter.get()
      log.info(s"Current queue size: ${linkedBlockingQueue.size()}, deliver rate: ${(cur - oldDeliverCounter.get()) / periodic * 100}")
      oldDeliverCounter.set(cur)
    case PeriodicM =>
      loadDataFromRedis()
    case ForceGetData =>
      loadDataFromRedis()
    case NeedMoreMsg =>
      deliverMsg(sender())
    case Finish(url, domain) =>
      RateLimiter.returnPermit(domain)
      //RateLimiter.printCounter(domain)
      deliverMsg(sender())
  }

  private[this] def deliverMsg(sender: ActorRef): Unit = {
    popQueue match {
      case Some(url) =>
        val duo = Helper.getDomainProtocol(url)
        duo match {
          case Some(du) =>
            if (RateLimiter.tryAcquire(du._1)) {
              //println ("here1 : " + du._1)
              deliverCounter.incrementAndGet()
              sender ! CrawlingUrl(du._2, du._1, 1)
            } else {
              //println("here2: " + du._1)
              RateLimiter.printCounter(du._1)
              pushQueue(url)
              deliverMsg(sender)
            }
          case None => self ! ForceGetData
        }
      case None => self ! ForceGetData
    }
  }

  private[this] def popQueue = Option(linkedBlockingQueue.poll())

  private[this] def pushQueue(item: String) = linkedBlockingQueue.offer(item)

  private[this] def loadDataFromRedis() = {
    1.to(batchSize).filter(_ => linkedBlockingQueue.size() < MAX_QUEUE_SIZE).foreach(_ => {
      val i = redisQueue.popQueue(RedisQueue.getDataFromSingle)
      i match {
        case Some(url) => linkedBlockingQueue.add(url)
        case None =>
      }
    })
  }

}
