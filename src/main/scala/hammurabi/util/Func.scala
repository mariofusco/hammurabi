package hammurabi.util

/**
 * @author Mario Fusco
 */
object Func {

  implicit def toListHelper[A](l: List[A]) = new {
    def +?[B <: A] (item: Option[B]) = item match {
      case Some(b) => b :: l
      case None => l
    }
  }
}