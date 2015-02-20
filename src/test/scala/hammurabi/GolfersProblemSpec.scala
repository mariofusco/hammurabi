package hammurabi

import org.scalatest.{FlatSpec, Matchers}
import hammurabi.Rule._

/**
 * @author Mario Fusco
 * @author nick
 * @since 20/02/15
 */
class GolfersProblemSpec extends FlatSpec with Matchers {
  "Vanilla golfers' Problem" should "succeed" in {

    var availablePos = (1 to 4).toSet
    var availableColors = Set("blue", "plaid", "red", "orange")

    val ruleSet = Set(
      rule("Unique positions") let {
        val p = any(kindOf[MutablePerson])
        when {
          (availablePos.size equals 1) and (p.pos equals 0)
        } then {
          p.pos = availablePos.head
        }
      },

      rule("Unique colors") let {
        val p = any(kindOf[MutablePerson])
        when {
          (availableColors.size equals 1) and (p.color == null)
        } then {
          p.color = availableColors.head
        }
      },

      rule("Joe is in position 2") let {
        val p = any(kindOf[MutablePerson])
        when {
          p.name equals "Joe"
        } then {
          p.pos = 2
          availablePos = availablePos - p.pos
        }
      },

      rule("Person to Fred’s immediate right is wearing blue pants") let {
        val p1 = any(kindOf[MutablePerson])
        val p2 = any(kindOf[MutablePerson])
        when {
          (p1.name equals "Fred") and (p2.pos equals p1.pos + 1)
        } then {
          p2.color = "blue"
          availableColors = availableColors - p2.color
        }
      },

      rule("Fred isn't in position 4") let {
        val possibleFredPos = availablePos - 4
        val p = any(kindOf[MutablePerson])
        when {
          (p.name equals "Fred") and (possibleFredPos.size == 1)
        } then {
          p.pos = possibleFredPos.head
          availablePos = availablePos - p.pos
        }
      },

      rule("Tom isn't in position 1 or 4") let {
        val possibleTomPos = availablePos - 1 - 4
        val p = any(kindOf[MutablePerson])
        when {
          (p.name equals "Tom") and (possibleTomPos.size equals 1)
        } then {
          p.pos = possibleTomPos.head
          availablePos = availablePos - p.pos
        }
      },

      rule("Bob is wearing plaid pants") let {
        val p = any(kindOf[MutablePerson])
        when {
          p.name equals "Bob"
        } then {
          p.color = "plaid"
          availableColors = availableColors - p.color
        }
      },

      rule("Tom isn't wearing orange pants") let {
        val possibleTomColors = availableColors - "orange"
        val p = any(kindOf[MutablePerson])
        when {
          (p.name equals "Tom") and (possibleTomColors.size equals 1)
        } then {
          p.color = possibleTomColors.head
          availableColors = availableColors - p.color
        }
      }
    )

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val bob = new MutablePerson("Bob")
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

  "Sugared golfers' problem" should "succeed" in {

    var availablePos = (1 to 4).toSet
    var availableColors = Set("blue", "plaid", "red", "orange")

    val assign = new {
      def color(color: String) = new {
        def to(person: MutablePerson) = {
          person.color = color
          availableColors = availableColors - color
        }
      }

      def position(pos: Int) = new {
        def to(person: MutablePerson) = {
          person.pos = pos
          availablePos = availablePos - pos
        }
      }
    }

    val ruleSet = Set(
      rule("Unique positions") let {
        val p = any(kindOf[MutablePerson])
        when {
          (availablePos.size equals 1) and (p.pos equals 0)
        } then {
          assign position availablePos.head to p
        }
      },

      rule("Unique colors") let {
        val p = any(kindOf[MutablePerson])
        when {
          (availableColors.size equals 1) and (p.color == null)
        } then {
          assign color availableColors.head to p
        }
      },

      rule("Joe is in position 2") let {
        val p = any(kindOf[MutablePerson])
        when {
          p.name equals "Joe"
        } then {
          assign position 2 to p
        }
      },

      rule("Person to Fred’s immediate right is wearing blue pants") let {
        val p1 = any(kindOf[MutablePerson])
        val p2 = any(kindOf[MutablePerson])
        when {
          (p1.name equals "Fred") and (p2.pos equals p1.pos + 1)
        } then {
          assign color "blue" to p2
        }
      },

      rule("Fred isn't in position 4") let {
        val possibleFredPos = availablePos - 4
        val p = any(kindOf[MutablePerson])
        when {
          (p.name equals "Fred") and (possibleFredPos.size == 1)
        } then {
          assign position possibleFredPos.head to p
        }
      },

      rule("Tom isn't in position 1 or 4") let {
        val possibleTomPos = availablePos - 1 - 4
        val p = any(kindOf[MutablePerson])
        when {
          (p.name equals "Tom") and (possibleTomPos.size equals 1)
        } then {
          assign position possibleTomPos.head to p
        }
      },

      rule("Bob is wearing plaid pants") let {
        val p = any(kindOf[MutablePerson])
        when {
          p.name equals "Bob"
        } then {
          assign color "plaid" to p
        }
      },

      rule("Tom isn't wearing orange pants") let {
        val possibleTomColors = availableColors - "orange"
        val p = any(kindOf[MutablePerson])
        when {
          (p.name equals "Tom") and (possibleTomColors.size equals 1)
        } then {
          assign color possibleTomColors.head to p
        }
      }
    )

    val tom = new MutablePerson("Tom")
    val joe = new MutablePerson("Joe")
    val fred = new MutablePerson("Fred")
    val bob = new MutablePerson("Bob")

    RuleEngine(ruleSet) execOn WorkingMemory(tom, joe, fred, bob)

    tom.pos shouldBe 3
    tom.color shouldBe "red"
    joe.pos shouldBe 2
    joe.color shouldBe "blue"
    fred.pos shouldBe 1
    fred.color shouldBe "orange"
    bob.pos shouldBe 4
    bob.color shouldBe "plaid"
  }

  "Immutable golfers' problem" should "succeed" in {
    var availablePos = (1 to 4).toSet
    var availableColors = Set("blue", "plaid", "red", "orange")

    val assign = new {
      def color(color: String) = new {
        def to(person: ImmutablePerson) = {
          Rule.remove(person)
          Rule.produce(person.copy(color = color))
          availableColors = availableColors - color
        }
      }

      def position(pos: Int) = new {
        def to(person: ImmutablePerson) = {
          Rule.remove(person)
          Rule.produce(person.copy(pos = pos))
          availablePos = availablePos - pos
        }
      }
    }

    val ruleSet = Set(
      rule("Unique positions") let {
        val p = any(kindOf[ImmutablePerson])
        when {
          (availablePos.size equals 1) and (p.pos equals 0)
        } then {
          assign position availablePos.head to p
        }
      },

      rule("Unique colors") let {
        val p = any(kindOf[ImmutablePerson])
        when {
          (availableColors.size equals 1) and (p.color == null)
        } then {
          assign color availableColors.head to p
        }
      },

      rule("Joe is in position 2") let {
        val p = any(kindOf[ImmutablePerson])
        when {
          p.name equals "Joe"
        } then {
          assign position 2 to p
        }
      },

      rule("Person to Fred’s immediate right is wearing blue pants") let {
        val p1 = any(kindOf[ImmutablePerson])
        val p2 = any(kindOf[ImmutablePerson])
        when {
          (p1.name equals "Fred") and (p2.pos equals p1.pos + 1)
        } then {
          assign color "blue" to p2
        }
      },

      rule("Fred isn't in position 4") let {
        val possibleFredPos = availablePos - 4
        val p = any(kindOf[ImmutablePerson])
        when {
          (p.name equals "Fred") and (possibleFredPos.size == 1)
        } then {
          assign position possibleFredPos.head to p
        }
      },

      rule("Tom isn't in position 1 or 4") let {
        val possibleTomPos = availablePos - 1 - 4
        val p = any(kindOf[ImmutablePerson])
        when {
          (p.name equals "Tom") and (possibleTomPos.size equals 1)
        } then {
          assign position possibleTomPos.head to p
        }
      },

      rule("Bob is wearing plaid pants") let {
        val p = any(kindOf[ImmutablePerson])
        when {
          p.name equals "Bob"
        } then {
          assign color "plaid" to p
        }
      },

      rule("Tom isn't wearing orange pants") let {
        val possibleTomColors = availableColors - "orange"
        val p = any(kindOf[ImmutablePerson])
        when {
          (p.name equals "Tom") and (possibleTomColors.size equals 1)
        } then {
          assign color possibleTomColors.head to p
        }
      }
    )

    val workingMemory = WorkingMemory(ImmutablePerson("Tom"), ImmutablePerson("Joe"), ImmutablePerson("Fred"), ImmutablePerson("Bob"))
    RuleEngine(ruleSet) execOn workingMemory

    workingMemory.all(classOf[ImmutablePerson]).size shouldBe 4

    val tom = workingMemory.firstHaving[ImmutablePerson](_.name == "Tom").get
    val joe = workingMemory.firstHaving[ImmutablePerson](_.name == "Joe").get
    val fred = workingMemory.firstHaving[ImmutablePerson](_.name == "Fred").get
    val bob = workingMemory.firstHaving[ImmutablePerson](_.name == "Bob").get

    tom.pos shouldBe 3
    tom.color shouldBe "red"
    joe.pos shouldBe 2
    joe.color shouldBe "blue"
    fred.pos shouldBe 1
    fred.color shouldBe "orange"
    bob.pos shouldBe 4
    bob.color shouldBe "plaid"
  }
}
