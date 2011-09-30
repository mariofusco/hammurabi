package hammurabi

/**
 * @author Mario Fusco
 */

class Person(n: String) {
  val name = n
  var pos: Int = _
  var color: String = _

  override def toString() = name + " is in pos " +
          (if (pos == 0) "unknown" else pos) +
          " with color " +
          (if (color == null) "unknown" else color)

  override def equals(obj: Any) = obj match {
    case p: Person => p.name == name
    case _ => false
  }

  override def hashCode = name.hashCode
}
