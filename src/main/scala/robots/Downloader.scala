package robots

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
    val get = new HttpGet(url)
    //TODO find a different way to maintain @{HttpClientContext}
    val ctx = HttpClientContext.create()
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
    download(s"$domainUrl/$RB")
  }

}
