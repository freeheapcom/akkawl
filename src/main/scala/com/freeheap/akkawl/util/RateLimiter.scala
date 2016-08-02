package com.freeheap.akkawl.util

import java.util.concurrent.atomic.AtomicInteger

/**
  * Created by minhdo on 8/1/16.
  */
object RateLimiter {
  private final val DEFAULT_MAX_ALLOWED_REQUEST = 20

  private val rates = new scala.collection.mutable.HashMap[String, AtomicInteger]


  def tryAcquire(domain: String) : Boolean = {
    val counter = rates.getOrElseUpdate(domain, new AtomicInteger(1))
    if (counter.getAndIncrement() < DEFAULT_MAX_ALLOWED_REQUEST)
       return true
    else {
       counter.decrementAndGet()
       return false
    }
  }

  def returnPermit(domain: String) = {
    val counter = rates.getOrElseUpdate(domain, new AtomicInteger(1))
    counter.decrementAndGet()
  }

  def printCounter(domain: String) = {
     println(domain + ": " + rates.getOrElseUpdate(domain, new AtomicInteger(1)).get())
  }
}
