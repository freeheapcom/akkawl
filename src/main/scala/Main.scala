import java.util.Properties

import akka.actor.{Actor, ActorSystem, AllDeadLetters, Props}
import akka.routing.RoundRobinPool
import com.freeheap.akkawl.core.{Coordinator, Crawler, DataParser, Loader}
import com.freeheap.akkawl.message.Finish
import com.freeheap.akkawl.util.ConfigLoader
import com.freeheap.drawler.dao.CrawlerDataStorage

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

  class Listener extends Actor {
    def receive = {
      case Finish(url, domain) => println(s"${self.path.name} is listening to: ${url}")
    }
  }

  private[this] def startup(props: Properties): Unit = {
    val redisHost = props.getProperty("redis.hosts", "localhost:6379")
    val rQueue = props.getProperty("redis.unprocessed_queue", "queue")
    val rSet = props.getProperty("redis.processed_set", "urls")
    val rHash = props.getProperty("redis.hash", "robots")

    val casConnStr = props.getProperty("cas.conn", "localhost:9042")
    val casKs = props.getProperty("cas.ks", "freeheap")
    val casTbl = props.getProperty("cas.tbl", "crawled_data")

    val qBatchSize = props.getProperty("system.q.batchSize", "10").toInt
    val crSize = props.getProperty("system.cr.size", "2").toInt
    val prSize = props.getProperty("system.pr.size", "2").toInt
    val ldSize = props.getProperty("system.ld.size", "6").toInt

    val respectRobot = props.getProperty("crawler.respectRobot", "true").toBoolean


    val coordPeriodic = props.getProperty("coord.periodic", "500").toInt

    val se = CrawlerDataStorage(casConnStr, casKs, casTbl)

    val system = ActorSystem("crawler-system")
    val coord = system.actorOf(Coordinator(redisHost, rQueue, qBatchSize, coordPeriodic))
    val ldRouter = system.actorOf(RoundRobinPool(ldSize).props(Loader(redisHost, rQueue, rSet, se)))
    val prRouter = system.actorOf(RoundRobinPool(prSize).props(DataParser(ldRouter)))
    system.actorOf(RoundRobinPool(crSize).props(Crawler(coord, prRouter, redisHost, rSet, rHash)))

    //val listener = system.actorOf(Props(classOf[Listener], this))
    //system.eventStream.subscribe(listener, classOf[AllDeadLetters])
  }
}

