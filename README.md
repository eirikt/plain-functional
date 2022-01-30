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

That being said; There are some motivations for taking this library further.
E.g. is Vavr [rejecting](https://github.com/vavr-io/vavr/issues/20) adding monads like the Writer monad and the State monad.
Those are extremely useful monads that really may bring a semi-functional Java codebase closer to a pure functional implementation.

So, who knows, maybe this library develops into something useful in the future.

---

The state of this little library is:
<b>_Very alpha_</b>&mdash;<b>don't use it for anything serious</b>, use [Vavr](https://www.vavr.io) (or something in that ballpark) instead.

(Either way, this library will not be set to anywhere near version 1.0 before it is used in production for many months, in more than one application! If it ever will.)

---

### API documentation

[Javadoc](https://plain-functional-javadoc.vercel.app)

The Javadoc is all there is of documentation for the time being.

---

### Integration
You can use [JitPack](https://jitpack.io) to use _Plain functional_ in your Maven-based project.
Add JitPack to your repositories:
```xml
<repositories>
    ...
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
    ...
</repositories>
```
And then you add it to your project dependencies:
```xml
<dependencies>
    ...
    <dependency>
        <groupId>com.github.eirikt</groupId>
        <artifactId>plain-functional</artifactId>
        <version>0.2</version>
    </dependency>
    ...
</dependencies>
```

Or for the truly bold:
```xml
<dependencies>
    ...
    <dependency>
        <groupId>com.github.eirikt</groupId>
        <artifactId>plain-functional</artifactId>
        <version>master-SNAPSHOT</version>
    </dependency>
    ...
</dependencies>
```

---

### Usage

Build library artifact by:
```cmd
mvn package
```
Then a `JAR` file will be generated in `/target`

All tests (formal specifications) will be executed as well.

...

Build Javadoc by:
```cmd
mvn javadoc:javadoc
```
Then API documentation will be available in `/target/site/apidocs/`

---

### License

[MIT License](file://LICENSE.txt)
