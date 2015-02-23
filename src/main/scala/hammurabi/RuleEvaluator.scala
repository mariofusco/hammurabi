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
import hammurabi.Rule._
import hammurabi.util.Func._
import hammurabi.util.Logger

import scala.collection._
import scala.language.reflectiveCalls
import scala.util.control.Exception._

/**
 * @author Mario Fusco
 */
private[hammurabi] abstract class RuleManipulator(workingMemory: WorkingMemory) {
  protected def inContext[R](block: => R) = {
    evaluationContext.set(this)
    val result = block
    evaluationContext.set(null)
    result
  }

  def any[A](clazz: Class[A]): Option[A] = fetch(clazz)(workingMemory.all(clazz))

  def anyHaving[A](clazz: Class[A])(condition: A => Boolean): Option[A] = fetch(clazz)(workingMemory.allHaving(clazz)(condition))

  def +(item: Any) = workingMemory + item

  def -(item: Any) = workingMemory - item

  def exitWith(result: Any)

  protected def fetch[A](clazz: Class[A])(f: => List[A]): Option[A]
}

private[hammurabi] class RuleEvaluator(rule: Rule, workingMemory: WorkingMemory)
  extends RuleManipulator(workingMemory) with Logger {

  var executedSets = Set[RuleExecutionSet]()
  var currentExecutionSet: RuleExecutionSet = _

  var isFirstExection = true
  var valuesCombinator: ValuesCombinator = _

  def evaluate(): Either[Throwable, List[RuleExecutor]] = {
    allCatch[List[RuleExecutor]] either {
      var executors: List[RuleExecutor] = List[RuleExecutor]()
      initFirstExecution()
      executors = executors +? evalRule
      isFirstExection = false
      while (valuesCombinator.hasNext) executors = executors +? evalRule
      executors
    }
  }

  private def initFirstExecution() = {
    isFirstExection = true
    valuesCombinator = new ValuesCombinator
  }

  private def evalRule(): Option[RuleExecutor] = {
    currentExecutionSet = new RuleExecutionSet
    inContext {
      val ruleDef = rule.bind()
      if (currentExecutionSet.isEvaluable) toRuleExecutor(ruleDef, currentExecutionSet)
      else None
    }
  }

  private def toRuleExecutor(ruleDef: RuleDefinition[_], executionSet: RuleExecutionSet): Option[RuleExecutor] = {
    debug("EVAL " + rule + " on " + executionSet)
    if (!isRuleFinished && !executedSets.contains(executionSet) && ruleDef.condition())
      Some(new RuleExecutor(this, rule, workingMemory, executionSet))
    else
      None
  }

  def exitWith(result: Any) = throw new IllegalStateException("Cannot exit during evaluation phase")

  private[hammurabi] def registerExecution(executedSet: RuleExecutionSet) = {
    executedSets = executedSets + executedSet
  }

  private def isRuleFinished = !isFirstExection && !valuesCombinator.hasNext

  protected def fetch[A](clazz: Class[A])(f: => List[A]): Option[A] = {
    currentExecutionSet += (
      if (isFirstExection) valuesCombinator += f.asInstanceOf[Traversable[A]]
      else valuesCombinator.next(clazz)
      )
  }
}

private[hammurabi] class RuleExecutor(evaluator: RuleEvaluator, rule: Rule, workingMemory: WorkingMemory, executionSet: RuleExecutionSet)
  extends RuleManipulator(workingMemory) with Logger {

  val salience = rule.salience

  private val executionIterator = executionSet.toIterator()
  private var result: Option[Any] = None

  def execRule(): Option[Any] = {
    inContext {
      val ruleDef = rule.bind()
      if (executionSet.isEvaluableOn(workingMemory) && ruleDef.condition()) {
        info("EXEC " + rule + " on " + executionSet)
        ruleDef.execution()
        evaluator.registerExecution(executionSet)
      }
    }
    result
  }

  protected def fetch[A](clazz: Class[A])(f: => List[A]): Option[A] =
    executionIterator.next().asInstanceOf[Option[A]]

  override def exitWith(result: Any) = this.result = Some(result)
}

private class RuleExecutionSet {
  val executionSet = new mutable.ListBuffer[Option[_]]()

  def +=[A](item: Option[A]) = {
    executionSet += item;
    item
  }

  def toIterator(): Iterator[Option[_]] = executionSet.iterator

  override def equals(that: Any): Boolean = that match {
    case that: RuleExecutionSet =>
      if (this.executionSet.length == that.executionSet.length) {
        val thatIterator = that.executionSet.toIterator
        (true /: executionSet)(_ && _ == thatIterator.next)
      } else false
    case _ => false
  }

  override def hashCode = (0 /: executionSet)(_ + _.hashCode * 13)

  override def toString = "[" + executionSet.map(_.getOrElse("Nothing")).mkString(", ") + "]"

  def isEvaluable = !executionSet.contains(None)

  def isEvaluableOn(workingMemory: WorkingMemory) = executionSet forall (item => workingMemory.contains(item.get))
}

private class ValuesCombinator {
  var values = List[List[Any]]()
  lazy val valuesIterator = cartesianProduct(values).tail.flatten.toIterator
  var finished = false

  private[hammurabi] def hasNext = values.nonEmpty && !finished

  private[hammurabi] def +=[A](t: Traversable[A]): Option[A] = {
    values = t.toList :: values
    finished = t.isEmpty
    if (finished) None else Some(t.head)
  }

  private[hammurabi] def next[A](clazz: Class[A]): Option[A] = {
    finished = !valuesIterator.hasNext
    if (finished) None else Some(valuesIterator.next().asInstanceOf[A])
  }

  private def cartesianProduct[A <: Any](l: List[List[A]]) = (l :\ List(List[A]())) {
    (ys, xss) => xss flatMap (xs => ys map (y => y :: xs))
  }
}