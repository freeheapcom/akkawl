package com.freeheap.akkawl.downloader

import java.net.URLEncoder

import com.freeheap.akkawl.robots.WebURL
import com.freeheap.akkawl.util.Logging
import org.apache.http.{HttpResponse, HttpStatus}
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.util.EntityUtils


/**
  * Created by william on 7/21/16.
  */
object Downloader extends Logging {

  def newClient(): CloseableHttpClient = {
    val cm = new PoolingHttpClientConnectionManager()
    cm.setMaxTotal(100)
    HttpClients.custom()
      .setConnectionManager(cm)
      .build()
  }

  def download(client: CloseableHttpClient, domain: String, url: String): Option[String] = {
    download(client, HttpClientContext.create())(domain, url)
  }

  def normalizeUrl(domain: String, url: String) = {
    val trimmedUrl = url.trim
    val i = trimmedUrl.indexOf('/', domain.length - 2)
    if (i < 0) url
    else {
      val after = trimmedUrl.substring(i + 1)
      trimmedUrl.substring(0, i + 1) + URLEncoder.encode(after, "UTF-8")
    }
  }

  def download(client: CloseableHttpClient, ctx: HttpClientContext)(domain: String, iUrl: String): Option[String] = {
    var response: CloseableHttpResponse = null
    val url = normalizeUrl(domain, iUrl)
    try {
      logger.info(s"$iUrl -> $url")
      val get = new HttpGet(url)
      response = client.execute(get, ctx)
      val code = response.getStatusLine.getStatusCode
      if (code >= HttpStatus.SC_OK && code < HttpStatus.SC_MULTIPLE_CHOICES) {
        val entity = response.getEntity
        if (entity != null) {
          val bytes = EntityUtils.toByteArray(entity)
          return Option(new String(bytes))
        }
      } else if (code >= HttpStatus.SC_MOVED_PERMANENTLY && code < HttpStatus.SC_BAD_REQUEST) {
        var newUrl: String = null
        try {
          newUrl = response.getFirstHeader("Location").getValue
        } catch {
          case t: Throwable =>
            newUrl = null
        }
        if (newUrl != null)
          return download(client, ctx)(domain, newUrl)
      }
    } catch {
      case e: Throwable =>
        error(s"Cannot download from $url ($iUrl)", e)
    } finally {
      if (response != null)
        response.close()
    }
    None
  }

  final val RB = "robots.txt"

  /**
    * Download robots file
    *
    * @param domainUrl needs to regard the format: http://domainname.com
    * @return
    */
  def downloadRobots(domainUrl: String): Option[String] = {
    downloadRobots(HttpClientContext.create())(s"$domainUrl")
  }

  def downloadRobots(ctx: HttpClientContext)(domain: String): Option[String] = {
    download(newClient(), ctx)(domain, s"$domain/$RB")
  }

  def downloadRobots(client: CloseableHttpClient, ctx: HttpClientContext)(domain: String): Option[String] = {
    download(client, ctx)(domain, s"$domain/$RB")
  }
}
