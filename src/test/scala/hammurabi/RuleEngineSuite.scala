package hammurabi

import org.scalatest.junit.JUnitSuite
import org.junit.Test
import org.junit.Assert._

import Rule._

/**
 * @author Mario Fusco
 */

class RuleEngineSuite extends JUnitSuite {

  @Test def allItemsFromWorkingSetBelongToExpectedType = {
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val tom = new Person("Tom")
    val bob = new Person("Bob")

    val workingMemory = WorkingMemory(joe, fred, "fred") + tom + "tom" + bob

    val allPersons = workingMemory.all(classOf[Person])
    assert(allPersons forall (_.isInstanceOf[Person]))
    expect(4)(allPersons.length)

    val shortNamePersons = workingMemory.allHaving(classOf[Person])(_.name.length < 4)
    expect(3)(shortNamePersons.length)
  }

  @Test def applyedRule = {
    val joe = new Person("Joe")

    val r = rule("Joe is in position 2") let {
      val p: Person = joe
      when {
        p.name == "Joe"
      } then {
        p.pos = 2
      }
    }

    assert(joe.pos != 2)
    r.exec
    assert(joe.pos == 2)
  }

  @Test def notApplyedRule = {
    val fred = new Person("Fred")

    val r = rule("Joe is in position 2") let {
      val p: Person = fred
      when {
        p.name == "Joe"
      } then {
        p.pos = 2
      }
    }

    assert(fred.pos != 2)
    r.exec
    assert(fred.pos != 2)
  }

  @Test def applyedRuleInRuleEngine = {
    val joe = new Person("Joe")

    val r = rule("Joe is in position 2") let {
      val p: Person = joe
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

  @Test def singleRuleInRuleEngine = {
    val r = rule("Joe is in position 2") let {
      val p = any(kindOf[Person])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
      }
    }

    val ruleEngine = RuleEngine(r)

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val bob = new Person("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(joe.pos != 2)
    assert(fred.pos != 2)

    ruleEngine execOn workingMemory

    assert(fred.pos != 2)
    assert(joe.pos == 2)
  }

  @Test def failingRuleInRuleEngine = {
    val r = rule("Joe is in position 2") let {
      val p = any(kindOf[Person])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
        failWith("This rule must fail!")
      }
    }

    val ruleEngine = RuleEngine(r)

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val bob = new Person("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    try {
      val res = RuleEngine(r) execOn workingMemory
      fail("This Executiona must fail")
    } catch {
      case e: FailedExecutionException => assert(true)
      case _ => fail("This Executiona must fail with a FailedExecutionException")
    }
  }

  @Test def singleConditionedRuleInRuleEngine = {
    val r = rule("Joe is in position 2") let {
      val p = kindOf[Person] having (_.name == "Joe")
      then {
        p.pos = 2
      }
    }

    val ruleEngine = RuleEngine(r)

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val bob = new Person("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(joe.pos != 2)
    assert(fred.pos != 2)

    ruleEngine execOn workingMemory

    assert(fred.pos != 2)
    assert(joe.pos == 2)
  }

  @Test def singleCombinedRuleInRuleEngine = {
    val r = rule("Person to Joe's immediate right is wearing blue pants") let {
      val p1 = any(kindOf[Person])
      val p2 = any(kindOf[Person])
      when {
        (p1.name equals "Joe") && (p2.pos equals p1.pos + 1)
      } then {
        p2.color = "blue"
      }
    }

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val bob = new Person("Bob")
    joe.pos = 2
    fred.pos = 3
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(fred.color != "blue")
    RuleEngine(r) execOn workingMemory
    assert(fred.color == "blue")
  }

  @Test def multipleAndCombinedRules = {
    val rule1 = rule("Joe is in position 2") let {
      val p = any(kindOf[Person])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
      }
    }

    val rule2 = rule("Fred is in position 3") let {
      val p = any(kindOf[Person])
      when {
        p.name equals "Fred"
      } then {
        p.pos = 3
      }
    }

    val rule3 = rule("Person to Joe’s immediate right is wearing blue pants") let {
      val p1 = any(kindOf[Person])
      val p2 = any(kindOf[Person])
      when {
        (p1.name equals "Joe") && (p2.pos equals p1.pos + 1)
      } then {
        p2.color = "blue"
      }
    }

    val ruleEngine = RuleEngine(rule3, rule1, rule2)

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val bob = new Person("Bob")
    val workingMemory = WorkingMemory(Set(tom, joe, fred, bob))

    assert(fred.color != "blue")
    ruleEngine execOn workingMemory
    assert(fred.color == "blue")
  }

  @Test def multipleAndCombinedRulesProducingObjects = {
    val rule1 = rule("Joe is in position 2") let {
      val p = any(kindOf[Person])
      when {
        p.name equals "Joe"
      } then {
        p.pos = 2
        produce(new Person("Fred"))
      }
    }

    val rule2 = rule("Fred is in position 3") let {
      val p = any(kindOf[Person])
      when {
        p.name equals "Fred"
      } then {
        p.pos = 3
        remove(new Person("Bob"))
      }
    }

    val rule3 = rule("Person to Joe’s immediate right is wearing blue pants") let {
      val p1 = any(kindOf[Person])
      val p2 = any(kindOf[Person])
      when {
        (p1.name equals "Joe") && (p2.pos equals p1.pos + 1)
      } then {
        p2.color = "blue"
        remove(new Person("Tom"))
        exitWith(p2)
      }
    }

    val ruleEngine = RuleEngine(rule3, rule1, rule2)

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val bob = new Person("Bob")
    val workingMemory = WorkingMemory(tom, joe, bob)

    val result = ruleEngine.execOn(workingMemory).get
    assert((workingMemory all classOf[Person]).size == 2)
    val fred = (workingMemory all classOf[Person] filter (_.name == "Fred")).head
    assertEquals(fred, result)
    assert(fred.color == "blue")
  }

  @Test def golfersProblem = {
    var availablePos = (1 to 4).toSet
    var availableColors = Set("blue", "plaid", "red", "orange")

    val ruleSet = Set(
      rule("Unique positions") let {
        val p = any(kindOf[Person])
        when {
          (availablePos.size equals 1) and (p.pos equals 0)
        } then {
          p.pos = availablePos.head
        }
      },

      rule("Unique colors") let {
        val p = any(kindOf[Person])
        when {
          (availableColors.size equals 1) and (p.color == null)
        } then {
          p.color = availableColors.head
        }
      },

      rule("Joe is in position 2") let {
        val p = any(kindOf[Person])
        when {
          p.name equals "Joe"
        } then {
          p.pos = 2
          availablePos = availablePos - p.pos
        }
      },

      rule("Person to Fred’s immediate right is wearing blue pants") let {
        val p1 = any(kindOf[Person])
        val p2 = any(kindOf[Person])
        when {
          (p1.name equals "Fred") and (p2.pos equals p1.pos + 1)
        } then {
          p2.color = "blue"
          availableColors = availableColors - p2.color
        }
      },

      rule("Fred isn't in position 4") let {
        val possibleFredPos = availablePos - 4
        val p = any(kindOf[Person])
        when {
          (p.name equals "Fred") and (possibleFredPos.size == 1)
        } then {
          p.pos = possibleFredPos.head
          availablePos = availablePos - p.pos
        }
      },

      rule("Tom isn't in position 1 or 4") let {
        val possibleTomPos = availablePos - 1 - 4
        val p = any(kindOf[Person])
        when {
          (p.name equals "Tom") and (possibleTomPos.size equals 1)
        } then {
          p.pos = possibleTomPos.head
          availablePos = availablePos - p.pos
        }
      },

      rule("Bob is wearing plaid pants") let {
        val p = any(kindOf[Person])
        when {
          p.name equals "Bob"
        } then {
          p.color = "plaid"
          availableColors = availableColors - p.color
        }
      },

      rule("Tom isn't wearing orange pants") let {
        val possibleTomColors = availableColors - "orange"
        val p = any(kindOf[Person])
        when {
          (p.name equals "Tom") and (possibleTomColors.size equals 1)
        } then {
          p.color = possibleTomColors.head
          availableColors = availableColors - p.color
        }
      }
    )

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val bob = new Person("Bob")
    val workingMemory = WorkingMemory(tom, joe, fred, bob)

    RuleEngine(ruleSet) execOn workingMemory

    assert(tom.pos == 3)
    assert(tom.color == "red")
    assert(joe.pos == 2)
    assert(joe.color == "blue")
    assert(fred.pos == 1)
    assert(fred.color == "orange")
    assert(bob.pos == 4)
    assert(bob.color == "plaid")

    println(tom)
    println(joe)
    println(fred)
    println(bob)
  }

  @Test def golfersProblemSugared = {
    var availablePos = (1 to 4).toSet
    var availableColors = Set("blue", "plaid", "red", "orange")

    val assign = new {
      def color(color: String) = new {
        def to(person: Person) = {
          person.color = color
          availableColors = availableColors - color
        }
      }

      def position(pos: Int) = new {
        def to(person: Person) = {
          person.pos = pos
          availablePos = availablePos - pos
        }
      }
    }

    val ruleSet = Set(
      rule ("Unique positions") let {
        val p = any(kindOf[Person])
        when {
          (availablePos.size equals 1) and (p.pos equals 0)
        } then {
          assign position availablePos.head to p
        }
      },

      rule ("Unique colors") let {
        val p = any(kindOf[Person])
        when {
          (availableColors.size equals 1) and (p.color == null)
        } then {
          assign color availableColors.head to p
        }
      },

      rule ("Joe is in position 2") let {
        val p = any(kindOf[Person])
        when {
          p.name equals "Joe"
        } then {
          assign position 2 to p
        }
      },

      rule ("Person to Fred’s immediate right is wearing blue pants") let {
        val p1 = any(kindOf[Person])
        val p2 = any(kindOf[Person])
        when {
          (p1.name equals "Fred") and (p2.pos equals p1.pos + 1)
        } then {
          assign color "blue" to p2
        }
      },

      rule ("Fred isn't in position 4") let {
        val possibleFredPos = availablePos - 4
        val p = any(kindOf[Person])
        when {
          (p.name equals "Fred") and (possibleFredPos.size == 1)
        } then {
          assign position possibleFredPos.head to p
        }
      },

      rule ("Tom isn't in position 1 or 4") let {
        val possibleTomPos = availablePos - 1 - 4
        val p = any(kindOf[Person])
        when {
          (p.name equals "Tom") and (possibleTomPos.size equals 1)
        } then {
          assign position possibleTomPos.head to p
        }
      },

      rule ("Bob is wearing plaid pants") let {
        val p = any(kindOf[Person])
        when {
          p.name equals "Bob"
        } then {
          assign color "plaid" to p
        }
      },

      rule ("Tom isn't wearing orange pants") let {
        val possibleTomColors = availableColors - "orange"
        val p = any(kindOf[Person])
        when {
          (p.name equals "Tom") and (possibleTomColors.size equals 1)
        } then {
          assign color possibleTomColors.head to p
        }
      }
    )

    val tom = new Person("Tom")
    val joe = new Person("Joe")
    val fred = new Person("Fred")
    val bob = new Person("Bob")

    RuleEngine(ruleSet) execOn WorkingMemory(tom, joe, fred, bob)

    assertEquals(3, tom.pos)
    assertEquals("red", tom.color)
    assertEquals(2, joe.pos)
    assertEquals("blue", joe.color)
    assertEquals(1, fred.pos)
    assertEquals("orange", fred.color)
    assertEquals(4, bob.pos)
    assertEquals("plaid", bob.color)

    println(tom)
    println(joe)
    println(fred)
    println(bob)
  }

  @Test def golfersProblemImmutable = {
    var availablePos = (1 to 4).toSet
    var availableColors = Set("blue", "plaid", "red", "orange")

    val assign = new {
      def color(color: String) = new {
        def to(person: IPerson) = {
          remove(person)
          produce(person.copy(color = color))
          availableColors = availableColors - color
        }
      }

      def position(pos: Int) = new {
        def to(person: IPerson) = {
          remove(person)
          produce(person.copy(pos = pos))
          availablePos = availablePos - pos
        }
      }
    }

    val ruleSet = Set(
      rule ("Unique positions") let {
        val p = any(kindOf[IPerson])
        when {
          (availablePos.size equals 1) and (p.pos equals 0)
        } then {
          assign position availablePos.head to p
        }
      },

      rule ("Unique colors") let {
        val p = any(kindOf[IPerson])
        when {
          (availableColors.size equals 1) and (p.color == null)
        } then {
          assign color availableColors.head to p
        }
      },

      rule ("Joe is in position 2") let {
        val p = any(kindOf[IPerson])
        when {
          p.name equals "Joe"
        } then {
          assign position 2 to p
        }
      },

      rule ("Person to Fred’s immediate right is wearing blue pants") let {
        val p1 = any(kindOf[IPerson])
        val p2 = any(kindOf[IPerson])
        when {
          (p1.name equals "Fred") and (p2.pos equals p1.pos + 1)
        } then {
          assign color "blue" to p2
        }
      },

      rule ("Fred isn't in position 4") let {
        val possibleFredPos = availablePos - 4
        val p = any(kindOf[IPerson])
        when {
          (p.name equals "Fred") and (possibleFredPos.size == 1)
        } then {
          assign position possibleFredPos.head to p
        }
      },

      rule ("Tom isn't in position 1 or 4") let {
        val possibleTomPos = availablePos - 1 - 4
        val p = any(kindOf[IPerson])
        when {
          (p.name equals "Tom") and (possibleTomPos.size equals 1)
        } then {
          assign position possibleTomPos.head to p
        }
      },

      rule ("Bob is wearing plaid pants") let {
        val p = any(kindOf[IPerson])
        when {
          p.name equals "Bob"
        } then {
          assign color "plaid" to p
        }
      },

      rule ("Tom isn't wearing orange pants") let {
        val possibleTomColors = availableColors - "orange"
        val p = any(kindOf[IPerson])
        when {
          (p.name equals "Tom") and (possibleTomColors.size equals 1)
        } then {
          assign color possibleTomColors.head to p
        }
      }
    )

    val workingMemory = WorkingMemory(IPerson("Tom"), IPerson("Joe"), IPerson("Fred"), IPerson("Bob"))
    RuleEngine(ruleSet) execOn workingMemory

    assertEquals(4, workingMemory.all(classOf[IPerson]).size)

    val tom = workingMemory.firstHaving[IPerson](_.name == "Tom").get
    val joe = workingMemory.firstHaving[IPerson](_.name == "Joe").get
    val fred = workingMemory.firstHaving[IPerson](_.name == "Fred").get
    val bob = workingMemory.firstHaving[IPerson](_.name == "Bob").get

    assertEquals(3, tom.pos)
    assertEquals("red", tom.color)
    assertEquals(2, joe.pos)
    assertEquals("blue", joe.color)
    assertEquals(1, fred.pos)
    assertEquals("orange", fred.color)
    assertEquals(4, bob.pos)
    assertEquals("plaid", bob.color)

    println(tom)
    println(joe)
    println(fred)
    println(bob)
  }

  @Test def rulePriority = {
    val ruleSet = Set(
      rule ("Third rule") withSalience -1 let {
        val s = any(kindOf[String])
        then {
          if (s == "Two") exitWith("Success")
          else failWith("s must be Two and not " + s)
        }
      },

      rule ("First rule") withSalience 1 let {
        val s = any(kindOf[String])
        when {
          s == "Zero"
        } then {
          if (s == "Zero") {
            remove(s)
            produce("One")
          }
          else failWith("s must be Zero and not " + s)
        }
      },

      rule ("Second rule") let {
        val s = any(kindOf[String])
        when {
          s == "Zero" or s == "One"
        } then {
          if (s == "One") {
            remove(s)
            produce("Two")
          }
          else failWith("s must be One and not " + s)
        }
      }
    )

    val s = (RuleEngine(ruleSet) execOn WorkingMemory("Zero")).get
    assertEquals("Success", s)
  }


  @Test def rulePriorityWithInts = {
    val ruleSet = Set(
      rule ("Third rule") withSalience -1 let {
        val i = any(kindOf[Int])
        then {
          if (i == 2) exitWith("Success")
          else failWith("i must be 2 and not " + i)
        }
      },

      rule ("First rule") withSalience 1 let {
        val i = any(kindOf[Int])
        when {
          i == 0
        } then {
          if (i == 0) {
            remove(i)
            produce(1)
          }
          else failWith("i must be 0 and not " + i)
        }
      },

      rule ("Second rule") let {
        val i = any(kindOf[Int])
        when {
          i < 2
        } then {
          if (i == 1) {
            remove(i)
            produce(2)
          }
          else failWith("i must be 1 and not " + i)
        }
      }
    )

    val i = (RuleEngine(ruleSet) execOn WorkingMemory(0)).get
    assertEquals("Success", i)
  }
}

