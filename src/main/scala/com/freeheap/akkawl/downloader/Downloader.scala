package com.freeheap.akkawl.downloader

import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.util.EntityUtils


/**
  * Created by william on 7/21/16.
  */
object Downloader {

  val cm = new PoolingHttpClientConnectionManager()
  val client = HttpClients.custom()
    .setConnectionManager(cm)
    .build()
  cm.setMaxTotal(100)

  def download(url: String): Option[String] = {
    download(url, HttpClientContext.create())
  }

  def download(url: String, ctx: HttpClientContext): Option[String] = {
    val get = new HttpGet(url)
    //    val ctx = HttpClientContext.create()
    val response = client.execute(get, ctx)
    try {
      val code = response.getStatusLine.getStatusCode
      if (code >= HttpStatus.SC_OK && code < HttpStatus.SC_MULTIPLE_CHOICES) {
        val entity = response.getEntity
        if (entity != null) {
          val bytes = EntityUtils.toByteArray(entity)
          return Option(new String(bytes))
        }
      }
    } catch {
      case e: Exception =>
    } finally {
      response.close()
    }

    None
  }

  final val RB = "robots.txt"

  /**
    * Download robots file
    *
    * @param domainUrl needs to regard the format: http://domainname
    * @return
    */
  def downloadRobots(domainUrl: String): Option[String] = {
    downloadRobots(s"$domainUrl/$RB", HttpClientContext.create())
  }

  def downloadRobots(domainUrl: String, ctx: HttpClientContext): Option[String] = {
    download(s"$domainUrl/$RB", ctx)
  }

  //
  //  def main(args: Array[String]) = {
  //    var start = System.currentTimeMillis()
  //    downloadRobots("https://tinhte.vn")
  //    var end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //    start = System.currentTimeMillis()
  //    downloadRobots("http://vnexpress.net")
  //    end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //    start = System.currentTimeMillis()
  //    downloadRobots("http://dantri.com.vn")
  //    end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //    start = System.currentTimeMillis()
  //    downloadRobots("https://github.com")
  //    end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //    start = System.currentTimeMillis()
  //    downloadRobots("https://github.com")
  //    end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //    start = System.currentTimeMillis()
  //    downloadRobots("https://github.com")
  //    end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //    start = System.currentTimeMillis()
  //    downloadRobots("https://github.com")
  //    end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //    start = System.currentTimeMillis()
  //    downloadRobots("https://www.quora.com")
  //    end = System.currentTimeMillis()
  //    println(s"${end - start}")
  //
  //
  //  }

}
