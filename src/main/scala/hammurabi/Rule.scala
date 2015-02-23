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
import scala.language.implicitConversions

/**
 * @author Mario Fusco
 */

case class Rule(description: String, bind: () => RuleDefinition[_], salience: Int = 0) {

  def exec() = {
    val ruleDef = bind()
    if (ruleDef.condition()) ruleDef.execution()
  }

  override def toString = "Rule: \"" + description + "\""
}

private[hammurabi] case class RuleDefinition[A](condition: () => Boolean, execution: () => A)

object Rule {

  type EvaluationContext = ThreadLocal[RuleManipulator]
  private[hammurabi] val evaluationContext = new EvaluationContext
  def currentContext = evaluationContext.get()

  def rule(s: String) = new {
    def let(letClause: => RuleDefinition[_]) = Rule(s, letClause _)
    def withSalience(salience: Int) = new {
      def let(letClause: => RuleDefinition[_]) = Rule(s, letClause _, salience)
    }
  }

  def when(condition: => Boolean) = new {
    def then[A](execution: => A) = RuleDefinition(condition _, execution _)
  }
  def then[A](execution: => A) = RuleDefinition(() => true, execution _)

  implicit def toSugaredBoolean(b1: Boolean) = new {
    def and(b2: Boolean) = b1 && b2
    def or(b2: Boolean) = b1 || b2
  }

  def kindOf[A](implicit manifest: Manifest[A]) = manifest.runtimeClass.asInstanceOf[Class[A]]
  def any[A](clazz: Class[A]): A = currentContext any clazz getOrElse null.asInstanceOf[A]
  implicit def toConditionedAny[A](clazz: Class[A]) = new {
    def having(condition: A => Boolean): A = currentContext.anyHaving(clazz)(condition) getOrElse null.asInstanceOf[A]
  }

  def produce(item: Any) = currentContext + item
  def remove(item: Any) = currentContext - item

  def exitWith(result: Any) = currentContext exitWith result
  def failWith(message: String) = throw FailedExecutionException(message)
}

case class FailedExecutionException(message: String) extends Exception {
  override def getLocalizedMessage = "Execution fail caused by: " + message
}