package robots

/**
  * Created by william on 7/21/16.
  */

trait Robots {
  /**
    * Download robots or get from cache
    *
    * @param domain
    * @return
    */
  def getRobots(domain: String): String

  /**
    * Parse and produce patterns which servers can block crawlers
    *
    * @param content
    * @return
    */
  def learnRobots(content: String): RuleSet

  /**
    * Check if we can crawl the specific url
    *
    * @param url
    * @return
    */
  def canCrawl(url: String): Boolean

  /**
    * Check if we can crawl the specific url without parsing domain from url
    *
    * @param domain
    * @param url
    * @return
    */
  def canCrawl(domain: String, url: String): Boolean

  /**
    * Clear local cache
    */
  def clearCache(): Unit

}
