# Plain functional Java

_**Disclaimer:**
I am not a functional programmer (yet), and took way too few math courses at university.
I am just a seasoned application-level software developer still looking for better ways._

After many years using object-oriented programming languages (OOP) in medium/large projects,
I am not happy with the current state of affairs.
Problematic properties of OOP are things like concurrency (shared mutable state), reuse, and scalability in general.

---

Functional programming concepts are powerful, yet so simple.
For an intro to functional programming, go e.g. [here](https://functionalprogramming.now.sh/1-functions-and-values.html) &mdash;my own attempt of clarifying concepts and terms, from the ground up.
_NB! Still work in progress._

---

This little library is one of my own stepping stones, going from the "imperative world" to the "functional, declarative world".

So why do this in Java?
Well, it's just the programming language I have mostly used the last years.
When possible, you should always try out new stuff in a familiar setting.
The differences stand out more clearly then.

In this library [Haskell](https://www.haskell.org) concepts and terminology will be followed.
Haskell again, has a strong mathematical foundation,
so hence the name "plain functional".
Also, that is why we are using (the good ol') Java 8 in this library&mdash;as we only need Java's [function type](https://en.wikipedia.org/wiki/Function_type).

The state of this little library is:
Don't use this, use [Vavr](https://www.vavr.io) (or something in that ballpark) instead.
But who knows, maybe this library develops into something that can be recommended in the future.

---

### Usage

Build library artifact by:
```cmd
mvn clean install
```
Then `JAR` file in `/target`

Generate Javadoc by:
```cmd
mvn javadoc:javadoc
```
Then open `/target/site/apidocs/index.html`
