package robots

import java.io.{FileInputStream, _}
import java.net.{MalformedURLException, URL}

import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by william on 7/21/16. Converted from TLDList ò crawler4j
  * This class is a singleton which obtains a list of TLDs (from online or a local file) in order to compare against
  * those TLDs
  */
class TLDList {}

object TLDList {
  private val TLD_NAMES_ONLINE_URL: String = "https://publicsuffix.org/list/effective_tld_names.dat"
  private val TLD_NAMES_TXT_FILENAME: String = "tld-names.txt"
  private val logger: Logger = LoggerFactory.getLogger(classOf[TLDList])
  private var onlineUpdate: Boolean = false
  private val tldSet = new java.util.HashSet[String](10000)
  init()

  def setUseOnline(online: Boolean) {
    onlineUpdate = online
  }

  def contains(str: String): Boolean = {
    tldSet.contains(str)
  }

  private[this] def init(): Unit = {
    if (onlineUpdate) {
      var url: URL = null
      try {
        url = new URL(TLD_NAMES_ONLINE_URL)
      } catch {
        case e: MalformedURLException =>
          logger.error("Invalid URL: {}", TLD_NAMES_ONLINE_URL)
          throw new RuntimeException(e)
      }
      val stream: InputStream = url.openStream
      try {
        logger.debug("Fetching the most updated TLD list online")
        val n: Int = readStream(stream)
        logger.info("Obtained {} TLD from URL {}", n, TLD_NAMES_ONLINE_URL)
        return
      } catch {
        case e: Exception =>
          logger.error(s"Couldn't fetch the online list of TLDs from: $TLD_NAMES_ONLINE_URL", e)

      } finally {
        if (stream != null) stream.close()
      }
    }

    val f: File = new File(TLD_NAMES_TXT_FILENAME)
    if (f.exists) {
      logger.debug("Fetching the list from a local file {}", TLD_NAMES_TXT_FILENAME)
      val tldFile: InputStream = new FileInputStream(f)
      try {
        val n: Int = readStream(tldFile)
        logger.info("Obtained {} TLD from local file {}", n, TLD_NAMES_TXT_FILENAME)
        return
      } catch {
        case e: IOException =>
          logger.error("Couldn't read the TLD list from local file", e)
      } finally {
        if (tldFile != null) tldFile.close()
      }
    }
    val tldFile: InputStream = getClass.getClassLoader.getResourceAsStream(TLD_NAMES_TXT_FILENAME)
    try {
      val n: Int = readStream(tldFile)
      logger.info("Obtained {} TLD from packaged file {}", n, TLD_NAMES_TXT_FILENAME)
    } catch {
      case e: IOException =>
        logger.error("Couldn't read the TLD list from file")
        throw new RuntimeException(e)
    } finally {
      if (tldFile != null) tldFile.close()
    }
  }

  private def readStream(stream: InputStream) = {
    var reader: BufferedReader = null
    try {
      reader = new BufferedReader(new InputStreamReader(stream))
      var line: String = null
      line = reader.readLine
      while (line != null) {
        line = line.trim
        if (!(line.isEmpty || line.startsWith("//")))
          tldSet.add(line)
        line = reader.readLine
      }
    } catch {
      case e: IOException =>
        logger.warn("Error while reading TLD-list: {}", e.getMessage)
    } finally {
      reader.close()
    }
    tldSet.size()
  }
}
