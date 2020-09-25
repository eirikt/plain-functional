# Plain functional Java

Functional programming concepts are powerful, yet so simple.
For an intro to functional programming, go e.g. [here](https://functionalprogramming.now.sh/1-functions-and-values.html) &mdash;my own attempt of clarifying concepts and terms, from the ground up.
_NB! Still work in progress._

---

After many years using object-oriented programming languages (OOP) in medium/large projects,
I am not happy with the current state of affairs.
Problematic properties of OOP are things like concurrency (shared mutable state), reuse, and scalability in general.
This little library is one of my own stepping stones, going from the "imperative world" to the "functional, declarative world".

So why do this in Java?
Well, it's just the programming language I have mostly used the last years.
When possible, you should always try out new stuff in a familiar setting.
The differences stand out more clearly then.

In this library [Haskell](https://www.haskell.org) concepts and terminology will be followed.
Haskell again, has a strong mathematical foundation,
hence the name "plain functional".
Also, that is why (the good ol') Java 8 is used in this library&mdash;as we only need Java's [function type](https://en.wikipedia.org/wiki/Function_type).

The state of this little library is:
<b>Don't use it</b>, use [Vavr](https://www.vavr.io) (or something in that ballpark) instead.
But who knows, maybe this library develops into something that might be recommended in the future.

---

### API documentation

[Javadoc](https://plain-functional-javadoc.vercel.app/)

---

### Usage

Build library artifact by:
```cmd
mvn package
```
Then `JAR` file in `/target`

...

Generate Javadoc by:
```cmd
mvn javadoc:javadoc
```
Then open `/target/site/apidocs/index.html`
