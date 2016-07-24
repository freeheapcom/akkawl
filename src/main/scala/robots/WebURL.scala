package robots

/**
  * Created by william on 7/21/16.
  */
class WebURL extends Serializable {
  var url: String = ""
  var docid: Int = -1
  var parentDocId: Int = -1
  var parentUrl: String = ""
  var depth: Short = -1
  var domain: String = ""
  var subDomain: String = ""
  var path: String = ""
  var anchor: String = ""
  var priority: Byte = -1
  var tag: String = ""
}


object WebURL {
  def apply(url: String) = {
    val urlEntity = new WebURL
    urlEntity.url = url

    val domainStartIdx: Int = url.indexOf("//") + 2
    var domainEndIdx: Int = url.indexOf('/', domainStartIdx)
    domainEndIdx = if (domainEndIdx > domainStartIdx) domainEndIdx
    else url.length
    var domain = url.substring(domainStartIdx, domainEndIdx)
    var subDomain = ""
    val parts: Array[String] = domain.split("\\.")
    if (parts.length > 2) {
      domain = parts(parts.length - 2) + "." + parts(parts.length - 1)
      var limit: Int = 2
      if (TLDList.contains(domain)) {
        domain = parts(parts.length - 3) + "." + domain
        limit = 3
      }
      for (i <- 0 to (parts.length - limit)) {
        if (!subDomain.isEmpty)
          subDomain += "."
        subDomain += parts(i)
      }
    }
    var path = url.substring(domainEndIdx)
    val pathEndIdx: Int = path.indexOf('?')
    if (pathEndIdx >= 0)
      path = path.substring(0, pathEndIdx)
    urlEntity.domain = domain
    urlEntity.subDomain = subDomain
    urlEntity.path = path

    urlEntity
  }
}