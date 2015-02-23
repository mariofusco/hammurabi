package hammurabi
/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
import hammurabi.util.Logger

import scala.collection.mutable
import scala.language.postfixOps

/**
 * @author Mario Fusco
 */

class WorkingMemory(var workingSet: List[_]) extends Logger {

  def this() = this(Nil)

  val workingSetsByType = new mutable.HashMap[Class[_], List[_]]

  def all[A](clazz: Class[A]): List[A] = {
    val c = normalizeClass(clazz)
    (workingSetsByType get c match {
      case objects: Some[_] => objects get
      case None =>
        val t = findObjectsOfClass(c)
        workingSetsByType += (c -> t)
        t
    }).asInstanceOf[List[A]]
  }
  def first[A](implicit manifest: Manifest[A]): Option[A] =
    firstOrNone(all(manifest.runtimeClass.asInstanceOf[Class[A]]))

  def allHaving[A](clazz: Class[A])(condition: A => Boolean): List[A] = {
    all(clazz) filter condition
  }

  def firstHaving[A](condition: A => Boolean)(implicit manifest: Manifest[A]): Option[A] =
    firstOrNone(allHaving(manifest.runtimeClass.asInstanceOf[Class[A]])(condition))

  private def firstOrNone[A](list: List[A]): Option[A] = list match {
    case x :: xs => Some(x)
    case _ => None
  }

  def +(item: Any) = {
    debug("Adding: " + item)
    addToInternalWorkingSets(item)
    this
  }

  def -(item: Any) = {
    debug("Removing: " + item)
    removeFromInternalWorkingSets(item)
    this
  }

  def contains(item: Any) = workingSet contains item

  private def addToInternalWorkingSets(item: Any) =
    modifyInternalWorkingSets(item)((objects, item) => item :: objects)

  private def removeFromInternalWorkingSets(item: Any) =
    modifyInternalWorkingSets(item)((objects, item) => objects filter (_ != item))

  private def modifyInternalWorkingSets(item: Any)(f: (List[_], Any) => List[_]) = {
    workingSet = f(workingSet, item)
    val clazz = normalizedClassOf(item)
    workingSetsByType += (clazz ->
      (workingSetsByType get clazz match {
        case Some(objects) => f(objects, item)
        case None => findObjectsOfClass(clazz)
      })
    )
  }

  private def findObjectsOfClass[A](clazz: Class[A]) = {
    workingSet filter (_.asInstanceOf[AnyRef].getClass == clazz)
  }

  private def normalizedClassOf(item: Any): Class[_] =
    if (classOf[AnyRef].isInstance(item)) item.asInstanceOf[AnyRef].getClass
    else if (classOf[Int].isInstance(item)) classOf[java.lang.Integer]
    else if (classOf[Long].isInstance(item)) classOf[java.lang.Long]
    else if (classOf[Double].isInstance(item)) classOf[java.lang.Double]
    else if (classOf[Float].isInstance(item)) classOf[java.lang.Float]
    else if (classOf[Char].isInstance(item)) classOf[java.lang.Character]
    else if (classOf[Byte].isInstance(item)) classOf[java.lang.Byte]
    else classOf[java.lang.Boolean]

  private def normalizeClass(c: Class[_]): Class[_] =
    if (classOf[AnyRef].isAssignableFrom(c)) c
    else if (c == classOf[Int]) classOf[java.lang.Integer]
    else if (c == classOf[Long]) classOf[java.lang.Long]
    else if (c == classOf[Double]) classOf[java.lang.Double]
    else if (c == classOf[Float]) classOf[java.lang.Float]
    else if (c == classOf[Char]) classOf[java.lang.Character]
    else if (c == classOf[Byte]) classOf[java.lang.Byte]
    else classOf[java.lang.Boolean]
}

object WorkingMemory {
  def apply() = new WorkingMemory()
  def apply(first: Any, workingSet: Any*) = new WorkingMemory(first :: workingSet.toList)
  def apply(workingSet: Traversable[_]) = new WorkingMemory(workingSet.toList)
}