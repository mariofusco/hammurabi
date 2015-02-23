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
import scala.annotation.tailrec
import scala.collection.parallel.ParIterable
import scala.collection._

/**
 * @author Mario Fusco
 */
class RuleEngine(rules: Traversable[Rule]) {

  var result: Option[Any] = None
  var error: Option[Throwable] = None
  var workingMemory: WorkingMemory = _

  def this(rules: Rule*) = this(rules.toTraversable)

  def execOn(workingMemory: WorkingMemory): Option[Any] = {
    this.workingMemory = workingMemory
    evaluate(rules.map(new RuleEvaluator(_, workingMemory)).par) fold (t => throw t, res => res)
  }

  @tailrec
  private def evaluate(evaluators: ParIterable[RuleEvaluator]): Either[Throwable, Option[Any]] = {
    sequence(evaluators.map(_.evaluate())) match {
      case Left(Nil) => throw new Exception("We should not be here!")
      case Left(e :: es) => Left(e)
      case Right(Nil) => Right(None)
      case Right(l) =>
        val execRes = l sortWith(_.salience > _.salience) map (_.execRule()) filter (_.isDefined)
        if (execRes.nonEmpty)
          Right(execRes.head) else evaluate(evaluators)
    }
  }

  private def sequence[A](t: GenIterable[Either[Throwable, List[A]]]): Either[List[Throwable], List[A]] = {
    var items: List[A] = Nil
    var errors: List[Throwable] = Nil
    t foreach {
      case Left(ex) => errors = ex :: errors
      case Right(l) => items = items ::: l
    }
    if (errors.isEmpty) Right(items) else Left(errors)
  }
}

object RuleEngine {
  def apply(rules: Rule*) = new RuleEngine(rules:_*)
  def apply(rules: Traversable[Rule]) = new RuleEngine(rules)
}

