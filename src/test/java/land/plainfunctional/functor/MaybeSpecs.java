package land.plainfunctional.functor;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import land.plainfunctional.testdomain.vanillaecommerce.MutableCustomer;

import static land.plainfunctional.functor.Maybe.just;
import static land.plainfunctional.functor.Maybe.nothing;
import static land.plainfunctional.functor.Maybe.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaybeSpecs {

    ///////////////////////////////////////////////////////////////////////////
    // Maybe semantics
    ///////////////////////////////////////////////////////////////////////////

    @Test
    void shouldEncapsulateValues() {
        Maybe<Integer> maybe3 = just(3);
        assertThat(maybe3.isNothing()).isFalse();

        assertThat(maybe3).isNotSameAs(just(3));
        assertThat(maybe3).isEqualTo(just(3));
    }

    @Test
    void whenJustIsNullValue_shouldThrowException() {
        assertThatThrownBy(
            () -> just(null)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null' value");
    }

    @Test
    void shouldEncapsulateNothing() {
        Maybe<String> nothing = nothing();
        assertThat(nothing.isNothing()).isTrue();
    }

    @Test
    void shouldEncapsulateNothingAndRespectReferentialTransparency() {
        Maybe<String> nothing = nothing();
        assertThat(nothing).isSameAs(nothing());
    }

    @Test
    void shouldEncapsulateNullValuesViaFactoryMethodOnly() {
        Maybe<String> maybe = of(null);
        assertThat(maybe.isNothing()).isTrue();
    }

    /**
     * @see <a href="http://blog.vavr.io/the-agonizing-death-of-an-astronaut/">Vavr blog</a>
     */
    @Test
    void shouldPreserveComputationalContext() {
        assertThatThrownBy(
            () -> of("someString").map((ignored) -> (Integer) null)

        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("Cannot create a 'Maybe.Just' from a 'null' value");
    }

    /**
     * NB! Protecting the integrity of the values is the values' responsibility! Sorry!
     *
     * TODO:
     * See the {@link land.plainfunctional.testdomain.vanillaecommerce} test package on examples how to do that.
     */
    @Test
    void willMutateArgumentsIfAllowedToDoThat() {
        MutableCustomer customer = new MutableCustomer();
        customer.customerId = "Jon";

        of(customer)
            .map((c) -> {
                c.customerId = "Lisa";
                return c;
            });

        assertThat(customer.customerId).isEqualTo("Lisa");
    }


    ///////////////////////////////////////////////////////////////////////////
    // Functor laws
    // See: https://wiki.haskell.org/Functor
    // See: http://eed3si9n.com/learning-scalaz/Functor+Laws.html
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Functors must preserve composition of morphisms:
     * map (g ∘ f) ≡ map g ∘ map f
     *
     * If two sequential mapping operations are performed one after the other using two functions,
     * the result should be the same as a single mapping operation with one function that is equivalent to applying the first function to the result of the second.
     */
    @Test
    void functorsShouldPreserveCompositionOfEndomorphisms_just() {
        Maybe<Integer> maybe3 = just(3);

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Maybe<Integer> F1 = maybe3.map(g.compose(f));
        Maybe<Integer> F2 = maybe3.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);

        // TODO: Include?
        //assertThat(F1.getOrDefault(0)).isEqualTo(3 + 13 - 5);
        //assertThat(F2.getOrDefault(0)).isEqualTo(3 + 13 - 5);

        // TODO: Include?
        //assertThat(F1._unsafe()).isEqualTo(3 + 13 - 5);
        //assertThat(F2._unsafe()).isEqualTo(3 + 13 - 5);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_just() {
        Maybe<Integer> maybe3 = just(13);

        Function<Object, String> intToString = Object::toString;
        Function<String, Integer> stringLength = String::length;

        Function<Object, String> f = intToString;
        Function<String, Integer> g = stringLength;

        Maybe<Integer> F1 = maybe3.map(g.compose(f));
        Maybe<Integer> F2 = maybe3.map(f).map(g);

        assertThat(F1).isNotSameAs(F2);
        assertThat(F1).isEqualTo(F2);

        // TODO: Include?
        //assertThat(F1.getOrDefault(0)).isEqualTo(2);
        //assertThat(F2.getOrDefault(0)).isEqualTo(2);

        // TODO: Include?
        //assertThat(F1._unsafe()).isEqualTo(2);
        //assertThat(F2._unsafe()).isEqualTo(2);
    }

    @Test
    void functorsShouldPreserveCompositionOfMorphisms_nothing() {
        Maybe<Integer> maybe = nothing();

        Function<Integer, Integer> plus13 = myInt -> myInt + 13;
        Function<Integer, Integer> minus5 = myInt -> myInt - 5;

        Function<Integer, Integer> f = plus13;
        Function<Integer, Integer> g = minus5;

        Maybe<Integer> F1 = maybe.map(g.compose(f));
        Maybe<Integer> F2 = maybe.map(f).map(g);

        assertThat(F1).isSameAs(F2);

        assertThat(F1.isNothing()).isTrue();
        assertThat(F2.isNothing()).isTrue();

        // TODO: Include?
        //assertThat(F1.getOrDefault(null)).isNull();
        //assertThat(F2.getOrDefault(null)).isNull();

        //assertThat(F1.getOrDefault(0)).isEqualTo(0);
        //assertThat(F2.getOrDefault(0)).isEqualTo(0);

        // TODO: Include?
        //assertThat(F1._unsafe()).isNull();
        //assertThat(F2._unsafe()).isNull();
    }

    /**
     * Functors must preserve identity morphisms:
     * map id ≡ id
     *
     * equivalent to (like in https://bartoszmilewski.com/2015/01/20/functors/):
     * F id_a ≡ id_F a
     *
     * When performing the mapping operation,
     * if the values in the functor are mapped to themselves,
     * the result will be an unmodified functor.
     */
    @Test
    void functorsShouldPreserveIdentityMorphism_just() {
        // F id_a
        Maybe<String> F_id_a = just(Function.<String>identity().apply("yes"));

        // id_F a
        Maybe<String> id_F_a = Function.<Maybe<String>>identity().apply(just("yes"));

        assertThat(F_id_a).isNotSameAs(id_F_a);
        assertThat(F_id_a).isEqualTo(id_F_a);

        // TODO: Include?
        // TODO: Move to own tests
        //assertThat(F_id_a._unsafe()).isSameAs(id_F_a._unsafe());
    }

    @Test
    void functorsShouldPreserveIdentityMorphism_nothing() {
        // F id_a
        Maybe<String> F_id_a = nothing();

        // id_F a
        Maybe<String> id_F_a = Function.<Maybe<String>>identity().apply(nothing());

        assertThat(F_id_a).isSameAs(id_F_a);
    }
}
