# Overview
One of the most common reason why software projects fail, or suffer unbearable delays, is the misunderstandings between
the analysts who define the business rules of the domain for which the software is going to be written and the developers
who have to put in code these rules. The latter write those rules in a language that is completely obscure for the first ones.
In this way the business analysts don't have a chance to read, understand and validate what the programmers developed and
then they can only empirically test the final software behavior, hardly covering all the possible corner cases and often
recognizing mistakes only when it is too late.


# What Hammurabi is
Hammurabi is a rule engine written in Scala that tries to leverage the features of this language making it particularly
suitable to implement extremely readable internal Domain Specific Languages. Indeed, what actually makes Hammurabi
different from all other rule engines is that it is possible to write and compile its rules directly in the host language.
Anyway, the Hammurabi's rules also have the important property of being readable even by non technical person. As usual,
a practical example worth more than a thousand words.

## The golfers problem
This logical puzzle has been taken from the first chapter of the Jess in Action book written by Ernest Friedman-Hill and published by Manning. It is described there as it follows:

 * A foursome of golfers is standing at a tee, in a line from left to right. Each golfer wears different colored pants; one
is wearing red pants.
 * The golfer to Fred’s immediate right is wearing blue pants.
 * Joe is second in line.
 * Bob is wearing plaid pants.
 * Tom isn’t in position one or four, and he isn’t wearing the hideous orange pants.

In what order will the four golfers tee off, and what color are each golfer’s pants?”

### The Jess solution
Jess is written in Java and is one of the most popular rule engine on the market. The solution to the golfers problems presented in the book mentioned above is the following: first it is necessary to define the data structures representing the problem

````
(deftemplate pants-color (slot of) (slot is))
(deftemplate position (slot of) (slot is))
A deftemplate is a bit like a class declaration in Java and in this case is used to write a first rule that in turns creates the facts representing each of the possible combinations of golfers, pants-color and positions:

(defrule generate-possibilities =>
  (foreach ?name (create$ Fred Joe Bob Tom)
    (foreach ?color (create$ red blue plaid orange)
      (assert (pants-color (of ?name)(is ?color)))
    )
    (foreach ?position (create$ 1 2 3 4)
       (assert (position (of ?name)(is ?position)))
    )
  )
)
````

After that it is possible to translate the sentences of the problem in the corresponding Jess rule:

````
(defrule find-solution
  ;; There is a golfer named Fred, whose position is ?p1
  ;; and pants color is ?c1
  (position (of Fred) (is ?p1))
  (pants-color (of Fred) (is ?c1))

  ;; The golfer to immediate right of Fred
  ;; is wearing blue pants.
  (position (of ?n&~Fred)(is ?p&:(eq ?p (+ ?p1 1))))
  (pants-color (of ?n&~Fred)(is blue&~?c1))

  ;; Joe is in position #2
  (position (of Joe) (is ?p2&2&~?p1))
  (pants-color (of Joe) (is ?c2&~?c1))

  ;; Bob is wearing the plaid pants
  (position (of Bob)(is ?p3&~?p1&~?p&~?p2))
  (pants-color (of Bob&~?n)(is plaid&?c3&~?c1&~?c2))

  ;; Tom is not in position 1 or 4
  ;; and is not wearing orange
  (position (of Tom&~?n)(is ?p4&~1&~4&~?p1&~?p2&~?p3))
  (pants-color (of Tom)(is ?c4&~orange&~blue&~?c1&~?c2&~?c3))

  =>
  (printout t Fred " " ?p1 " " ?c1 crlf)
  (printout t Joe " " ?p2 " " ?c2 crlf)
  (printout t Bob " " ?p3 " " ?c3 crlf)
  (printout t Tom " " ?p4 " " ?c4 crlf)
)
````

where the rows starting with ;; are just comments. In this way if you enter the code for the problem into Jess and then
run it, you get the answer directly:

````
Fred 1 orange
Joe 2 blue
Bob 4 plaid
Tom 3 red
````

Note that the facts that the golfers are in different positions and wear pants of different colors is not expressed in
an explicit rule but need to be spread and repeated in many statements. This solution is clearly difficult to be maintained
and doesn't scale well as underlined by the last condition statement stating that the position ?p4 of Tom is ?p4&~1&~4&~?p1&~?p2&~?p3 where ~ means not in the Jess language. In other words it says that the Tom's position is not only different from the position 1 and 4 but it is also different from the positions of all the other golfers (named one by one) formerly defined. Actually the needs to describe a golfer's position also as the negation of the positions of all the other golfers implies something even worse: it is not possible to translate each sentence of the problem in a different rule, but they have to be combined together in a single big rule. After this huge if part, its then section (the one after the => symbol) prints out a table containing the set of variables ?p1…?p4 and ?c1…?c4 that solves the problem.

### The Hammurabi solution
As done while presenting the Jess solution, also with Hammurabi the first thing to do is to define the domain of the problem. In order to do that, since the Hammurabi rules are valid Scala statements, it is sufficient to create a plain Scala Person class having as attributes the name, the position and the color of the pants of the golfer that it represents:

````
class Person(n: String) {
  val name = n
  var pos: Int = _
  var color: String = _
}
````

Then we can model the fact that all the golfers must have different position and pants color by putting them in 2 different Set:

````
var availablePos = (1 to 4).toSet
var availableColors = Set("blue", "plaid", "red", "orange")
````

and write two small methods that pull off them from the corresponding set once they have been assigned to a specific golfer:

````
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
````

Those methods are written in a quite weird way just to make even more readable the DSL that will be used to define the rules and to stress the idea that it is possible to write the rules in a valid Scala that could be easily understood by a non-technical person. Of course it is easy to go even further by encapsulating other concepts in some convenient methods as it has been done above. Now everything is ready to write the set of rules describing the golfers problem:

````
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
````

Here the first 2 rules explicitly leverage the uniqueness of positions and pants colors by assigning the last available
of them to the only person who still doesn't have one. The other rules just match one by one the sentences of the problem
as it has been defined. Now it is possible to make Hammurabi solve the problem by creating the four golfers:

````
val tom = new Person("Tom")
val joe = new Person("Joe")
val fred = new Person("Fred")
val bob = new Person("Bob")
````

add them to a working memory (the set of objects against which the rule engine will evaluate and fire the rules):

````
val workingMemory = WorkingMemory(tom, joe, fred, bob)
````

and letting the rule engine, initialized with the formerly defined set of rules, to work on it:

````
RuleEngine(ruleSet) execOn workingMemory
````

Working with immutable data structures
Immutability is probably not something that should be enforced at all costs in Hammurabi, for a very simple reason:
the largest part of the execution time is spent by Hammurabi, like all other rule engines, looking for rules that can
actually be executed (fired), i.e. the ones for which the when condition is true. During this phase data are only read and never written, so immutability doesn't matter at all, and all the rules that can be fired are put in an agenda. During the subsequent phase the rules in the agenda MUST be fired one by one, since the execution of one of them could make false the when condition of another one. It means that lack of immutability shouldn't prevent the rule engine to safely run in parallel during the discovery phase. That said, it is also possible to obtain the same result working with immutable data structures. For example having an immutable person:

````
case class Person(name: String, pos: Int = 0, color: String = null)
````

it's enough to rewrite the methods that assign the position and pants color to the golfers as it follows:

````
val assign = new {
  def color(color: String) = new {
    def to(person: Person) = {
      remove(person)
      produce(person.copy(color = color))
      availableColors = availableColors - color
    }
  }

  def position(pos: Int) = new {
    def to(person: Person) = {
      remove(person)
      produce(person.copy(pos = pos))
      availablePos = availablePos - pos
    }
  }
}
````

leaving all the rules unchanged. In this way the old version of the Person is removed from the working memory and a brand
new one, with its position or color set accordingly to the fired rule, is produced and then added to the working memory
itself. The methods remove and produce can be indeed used respectively to remove objects from the working memory and produce
new objects, that could be then used to evaluate and fire other rules.

### Hammurabi internals
In Hammurabi all the rules are evaluated (but not fired) in parallel basically by assigning each rule to a different Scala actor.
The replacement of this actor implementation with one based on the upcoming Scala parallel collections is currently under
evaluation but I decided to wait until this technology will be stable. As anticipated, the evaluation of the rule in parallel
is safe because is a read only process. While the actors discover set of variables that can fire the rules they are evaluating,
they add them to the rule engine's agenda. At the end of this evaluation process the rules are fired sequentially one by
one after a re-evaluation of their application condition, because the execution of one of them could change the result
of that condition for a subsequent one.

All the rules are fired in no particular order unless a different priority has been specified for some of them. Indeed
since sometimes there could be the need to treat some rules as special cases they have an optional property called salience
that acts as a priority setting for that rule in order to allow activated rules with the highest salience to always fire
first, followed by rules of lower salience. By default all rules have salience 0 but you can alter it in the rule
definition as it follows:

````
rule ("Important rule") withSalience 10 let { ... }
rule ("Negligible rule") withSalience -5 let { ... }
````

Each actor also records the set of values on which the rule it is responsible for has been already executed, in order to
not fire again the same rule on the same values. The evaluation phase and the firing one are then executed again and
again until either there is no rule that can be fired or one of the rule during its firing phase invokes one of the
methods exitWith, making the rule engine gracefully finishing returning a value representing the result of the whole
evaluation process or failWith that cause the rule engine to terminate by throwing an exception. Of course if the rule
engine stops just because there are no longer rules that can be fired, the result of the evaluation is represented by the
whole working set (as in the former example) and you can read from there the values you are interested in.

Further implementations and improvements
At the moment it is only possible to take from the working memory the object(s) against which a given rule will be evaluated
only selecting them by type, as shown in the statement:

````
val p = any(kindOf[Person])
````

Further mechanisms to categorize and select those objects in a more precise way are under evaluation, even if it is already
possible to limit them with a Boolean function as in the following example:

````
val p = kindOf[Person] having (_.name == "Joe")
````

This is useful also under a performance point of view because it dramatically lowers the number of combination that the
rule engine needs to check before to find a rule that can be fired. For example the "Person to Fred’s immediate right is
wearing blue pants" rule could be rewritten as it follows bringing the number of combination that has to be tried from 16 to 4:

````
rule ("Person to Fred’s immediate right is wearing blue pants") let {
  val p1 = kindOf[Person] having (_.name == "Fred")
  val p2 = any(kindOf[Person])
  when {
   p2.pos equals p1.pos + 1
  } then {
    assign color "blue" to p2
  }
}
````

I am also evaluating of directly feeding the working memory with a NoSQL database. In other words, with this solution,
the data present in the db could represent the working memory itself. At the moment I am experimenting with MongoDB since
is the one I know best, but if somebody has some other good idea or even better wants to collaborate with this project
I'd be very glad of it.

More in general, if you have any suggestion that could enrich the features or improve the quality of the source code,
the readability of the DSL or the performances of the rule engine, would be great if you could share them on the Issues
page of the Hammurabi project.