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
import org.scalatest.{FlatSpec, Matchers}

import scala.language.reflectiveCalls

/**
 * @author Mario Fusco
 * @author Nick Jacobs (port to ScalaTest)
 */

class RuleEngineSpec extends FlatSpec with Matchers {

  "All items from working set" should "belong to expected type" in {

    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val tom = new MutablePerson("Tom")
    val bob = new MutablePerson("Bob")

    val workingMemory = WorkingMemory(joe, fred, "fred") + tom + "tom" + bob

    val allPersons = workingMemory.all(classOf[MutablePerson])
    allPersons should have length 4
    allPersons shouldBe a[List[_]]

    val shortNamePersons = workingMemory.allHaving(classOf[MutablePerson])(_.name.length < 4)
    shortNamePersons should have length 3

  }

  "Simple rule test: Joe" should "be in position 2" in {

    val joe = new MutablePerson("Joe")

    val r = rule("Joe is in position 2") let {
      val p: MutablePerson = joe
      when {
        p.name == "Joe"
      } then {
        p.pos = 2
      }
    }

    assert(joe.pos != 2)
    r.exec()
    assert(joe.pos == 2)
  }

  "Simple rule test: Fred" should "not be in position 2" in {
    val fred = new MutablePerson("Fred")

    val r = rule("Joe is in position 2") let {
      val p: MutablePerson = fred
      when {
        p.name == "Joe"
      } then {
        p.pos = 2
      }
    }

    assert(fred.pos != 2)
    r.exec()
    assert(fred.pos != 2)
  }

  "Joe alone" should "be in position 2 in working memory" in {
    val joe = new MutablePerson("Joe")

    val r = rule("Joe is in position 2") let {
      val p: MutablePerson = joe
      when {
        p.name == "Joe"
      } then {
        p.pos = 2
      }
    }

    assert(joe.pos != 2)
    RuleEngine(r) execOn WorkingMemory(Set(joe))
    assert(joe.pos == 2)
  }

  "Joe with multiple persons" should "be in position 2 in working memory" in {

    val r = rule("Joe is in position 2") let {
      val p = any(kindOf[MutablePerson])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
      }
    }

    val ruleEngine = RuleEngine(r)

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val bob = new MutablePerson("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(joe.pos != 2)
    assert(fred.pos != 2)

    ruleEngine execOn workingMemory

    assert(fred.pos != 2)
    assert(joe.pos == 2)
  }

  "Failing condition" should "fail in rules engine" in {

    val r = rule("Joe is in position 2") let {
      val p = any(kindOf[MutablePerson])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
        failWith("This rule must fail!")
      }
    }

    val ruleEngine = RuleEngine(r)

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val bob = new MutablePerson("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    try {
      val res = RuleEngine(r) execOn workingMemory
      fail("This Executiona must fail")
    } catch {
      case e: FailedExecutionException => assert(true)
      case _: Throwable => fail("This Executiona must fail with a FailedExecutionException")
    }
  }

  "Single condition" should "succeed in rules engine" in {
    val r = rule("Joe is in position 2") let {
      val p = kindOf[MutablePerson] having (_.name == "Joe")
      then {
        p.pos = 2
      }
    }

    val ruleEngine = RuleEngine(r)

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val bob = new MutablePerson("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(joe.pos != 2)
    assert(fred.pos != 2)

    ruleEngine execOn workingMemory

    assert(fred.pos != 2)
    assert(joe.pos == 2)
  }

  "Single rule with combined conditions " should " succeed in rules engine" in {

    val r = rule("Person to Joe's immediate right is wearing blue pants") let {
      val p1 = any(kindOf[MutablePerson])
      val p2 = any(kindOf[MutablePerson])
      when {
        (p1.name equals "Joe") && (p2.pos equals p1.pos + 1)
      } then {
        p2.color = "blue"
      }
    }

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val bob = new MutablePerson("Bob")
    joe.pos = 2
    fred.pos = 3
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(fred.color != "blue")
    RuleEngine(r) execOn workingMemory
    assert(fred.color == "blue")
  }

  "Multiple and combined rules " should " succeed in rules engine" in {

    val rule1 = rule("Joe is in position 2") let {
      val p = any(kindOf[MutablePerson])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
      }
    }

    val rule2 = rule("Fred is in position 3") let {
      val p = any(kindOf[MutablePerson])
      when {
        p.name equals "Fred"
      } then {
        p.pos = 3
      }
    }

    val rule3 = rule("Person to Joe’s immediate right is wearing blue pants") let {
      val p1 = any(kindOf[MutablePerson])
      val p2 = any(kindOf[MutablePerson])
      when {
        (p1.name equals "Joe") && (p2.pos equals p1.pos + 1)
      } then {
        p2.color = "blue"
      }
    }

    val ruleEngine = RuleEngine(rule3, rule1, rule2)

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val bob = new MutablePerson("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(fred.color != "blue")
    ruleEngine execOn workingMemory
    assert(fred.color == "blue")
  }

  "Multiple and combined rules producing objects" should "succeed in rules engine" in {

    val rule1 = rule("Joe is in position 2") let {
      val p = any(kindOf[MutablePerson])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
        Rule.produce(new MutablePerson("Fred"))
      }
    }

    val rule2 = rule("Fred is in position 3") let {
      val p = any(kindOf[MutablePerson])
      when {
        p.name equals "Fred"
      } then {
        p.pos = 3
        Rule.remove(new MutablePerson("Bob"))
      }
    }

    val rule3 = rule("Person to Joe’s immediate right is wearing blue pants") let {
      val p1 = any(kindOf[MutablePerson])
      val p2 = any(kindOf[MutablePerson])
      when {
        (p1.name equals "Joe") && (p2.pos equals p1.pos + 1)
      } then {
        p2.color = "blue"
        Rule.remove(new MutablePerson("Tom"))
        Rule.exitWith(p2)
      }
    }

    val ruleEngine = RuleEngine(rule3, rule1, rule2)

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val bob = new MutablePerson("Bob")
    val workingMemory = WorkingMemory(tom, joe, bob)

    val result = ruleEngine.execOn(workingMemory).get
    assert((workingMemory all classOf[MutablePerson]).size == 2)
    val fred = (workingMemory all classOf[MutablePerson] filter (_.name == "Fred")).head
    fred shouldBe result
    fred.color shouldBe "blue"
  }
}





