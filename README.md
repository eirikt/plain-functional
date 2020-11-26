# Plain functional Java

Functional programming concepts are powerful, yet so simple.
For an intro to functional programming, go e.g. [here](https://functionalprogramming.now.sh/1-functions-and-values.html).
(My own attempt of clarifying concepts and terms, from the ground up.
_NB! Still work in progress._)

---

The introduction of [function types](https://en.wikipedia.org/wiki/Function_type) in [Java 8](https://en.wikipedia.org/wiki/Java_version_history#Java_SE_8) made it possible to create [higher-order functions](https://en.wikipedia.org/wiki/Higher-order_function), and apply concepts from [functional programming](https://en.wikipedia.org/wiki/Functional_programming) in Java the language.

The usual way of achieving this is by using [lambda expressions](https://en.wikipedia.org/wiki/Anonymous_function), the [Java Stream API](https://www.baeldung.com/java-8-streams), and/or using a third-party library like [Vavr](https://www.vavr.io).

This library is yet another of those third-party libraries...
So, isn't this just another blatant example of [not-invented-here](https://en.wikipedia.org/wiki/Not_invented_here) thinking ;-)
Well, this library is first and foremost a personal educational project.
I will strive to follow concepts and terminology from [Haskell](https://www.haskell.org).
Haskell has a strong mathematical foundation, hence the name "_Plain functional_".
This, again, is for the ultimate goal of precision and preciseness when declaring logic.
_Plain functional_ will prioritize ease-of-use and readability first&mdash;and performance later.
Who knows, maybe this library develops into something useful in the future.

---

The state of this little library is:
<b>_Very alpha_</b>&mdash;<b>don't use it for anything serious</b>, use [Vavr](https://www.vavr.io) (or something in that ballpark) instead.

(Either way, this library will not be set to anywhere near version 1.0 before it is used in production for many months, in more than one application! If it ever will.)

---

### API documentation

[Javadoc](https://plain-functional-javadoc.vercel.app)

The Javadoc is all there is of documentation for the time being.

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

---

### License

[MIT License](file://LICENSE.txt)
