import java.util.Properties

import akka.actor.ActorSystem
import akka.routing.RoundRobinPool
import com.freeheap.akkawl.core.{Coordinator, Crawler, DataParser, Loader}
import com.freeheap.akkawl.util.ConfigLoader
import com.freeheap.drawler.dao.{CrawlerDataStorage, LinkQueue, LinkSet}

/**
  * @author william
  */
object Main {
  def main(args: Array[String]): Unit = {
    val props = ConfigLoader.loadConfig("app.properties")

    println("Starting up")
    startup(props)
    println("Wait for termination....")
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() = {
        println("System is shutting down")
      }
    })
  }

  private[this] def startup(props: Properties): Unit = {
    val redisHost = props.getProperty("redis.hosts", "localhost:6379")
    val rQueue = props.getProperty("redis.queue", "queue")
    val rSet = props.getProperty("redis.set", "urls")
    val casConnStr = props.getProperty("cas.conn", "localhost:9042")
    val casKs = props.getProperty("cas.ks", "test")
    val casTbl = props.getProperty("cas.tbl", "test")

    val qBatchSize = props.getProperty("system.q.batchSize", "10").toInt
    val crSize = props.getProperty("system.cr.size", "2").toInt
    val prSize = props.getProperty("system.pr.size", "2").toInt
    val ldSize = props.getProperty("system.ld.size", "6").toInt

    val coordPeriodic = props.getProperty("coord.periodic", "500").toInt

    val se = CrawlerDataStorage(casConnStr, casKs, casTbl)

    val system = ActorSystem("crawler-system")
    val coord = system.actorOf(Coordinator(redisHost, rQueue, qBatchSize, coordPeriodic))
    val ldRouter = system.actorOf(RoundRobinPool(ldSize).props(Loader(redisHost, rQueue, rSet, se)))
    val prRouter = system.actorOf(RoundRobinPool(prSize).props(DataParser(ldRouter)))
    system.actorOf(RoundRobinPool(crSize).props(Crawler(coord, prRouter, redisHost, rSet)))
  }
}

