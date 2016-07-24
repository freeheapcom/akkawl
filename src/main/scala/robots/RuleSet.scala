package robots

import java.util.TreeSet

/**
  * Created by william on 7/21/16.
  */
class RuleSet extends TreeSet[String] with Serializable {
  override def add(str: String): Boolean = {
    var sub: java.util.SortedSet[String] = headSet(str)
    if (!sub.isEmpty && str.startsWith(sub.last))
      return false
    val retVal: Boolean = super.add(str)
    sub = tailSet(str + "\0")
    while (!sub.isEmpty && sub.first.startsWith(str))
      sub.remove(sub.first)
    retVal
  }

  def containsPrefixOf(s: String): Boolean = {
    val sub: java.util.SortedSet[String] = headSet(s)
    if (!sub.isEmpty && s.startsWith(sub.last))
      return true
    contains(s)
  }
}

object RuleSet {
  def apply() = new RuleSet

  final val DEF_UA = "freeheap-mark-III"

  def apply(data: String): RuleSet = {
    apply(data, DEF_UA)
  }

  def apply(data: String, uag: String): RuleSet = {
    Utils.disassemblyRobots(data, uag)
  }
}
