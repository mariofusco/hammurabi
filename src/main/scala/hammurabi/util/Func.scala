package hammurabi.util

import scala.language.implicitConversions

/**
 * @author Mario Fusco
 */
object Func {

  implicit def toListHelper[A](l: List[A]): Object {def +?[B <: A](item: Option[B]): List[A]} = new {
    def +?[B <: A] (item: Option[B]) = item match {
      case Some(b) => b :: l
      case None => l
    }
  }
}