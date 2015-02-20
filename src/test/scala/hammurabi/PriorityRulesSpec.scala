package hammurabi

import hammurabi.Rule._
import org.scalatest.{Matchers, FlatSpec}

/**
 * @author Mario Fusco
 * @author nick
 * @since 20/02/15
 */
class PriorityRulesSpec {
  class PriorityRules extends FlatSpec with Matchers {
    "Rule priority" should "succeed" in {
      val ruleSet = Set(
        rule("Third rule") withSalience -1 let {
          val s = any(kindOf[String])
          then {
            if (s == "Two") exitWith("Success")
            else failWith("s must be Two and not " + s)
          }
        },

        rule("First rule") withSalience 1 let {
          val s = any(kindOf[String])
          when {
            s == "Zero"
          } then {
            if (s == "Zero") {
              Rule.remove(s)
              Rule.produce("One")
            }
            else failWith("s must be Zero and not " + s)
          }
        },

        rule("Second rule") let {
          val s = any(kindOf[String])
          when {
            s == "Zero" or s == "One"
          } then {
            if (s == "One") {
              Rule.remove(s)
              Rule.produce("Two")
            }
            else failWith("s must be One and not " + s)
          }
        }
      )

      (RuleEngine(ruleSet) execOn WorkingMemory("Zero")).get shouldBe "Success"
    }

    "Rule priority with ints" should "succeed" in {

      val ruleSet = Set(
        rule("Third rule") withSalience -1 let {
          val i = any(kindOf[Int])
          then {
            if (i == 2) exitWith("Success")
            else failWith("i must be 2 and not " + i)
          }
        },

        rule("First rule") withSalience 1 let {
          val i = any(kindOf[Int])
          when {
            i == 0
          } then {
            if (i == 0) {
              Rule.remove(i)
              Rule.produce(1)
            }
            else failWith("i must be 0 and not " + i)
          }
        },

        rule("Second rule") let {
          val i = any(kindOf[Int])
          when {
            i < 2
          } then {
            if (i == 1) {
              Rule.remove(i)
              Rule.produce(2)
            }
            else failWith("i must be 1 and not " + i)
          }
        }
      )

      (RuleEngine(ruleSet) execOn WorkingMemory(0)).get shouldBe "Success"
    }
  }
}
