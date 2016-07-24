package robots

import com.freeheap.drawler.dao.RobotsHash
import com.freeheap.drawler.drivers.RedisConnection

/**
  * Created by william on 7/21/16.
  */
object RobotsFactory {

  def newRobotsChecker(rs: RobotsHash, rf: (RedisConnection, String, String) => Option[String],
                       rfa: (RedisConnection, String, String, String) => Unit) = new Checker(rs, rf, rfa)

}
