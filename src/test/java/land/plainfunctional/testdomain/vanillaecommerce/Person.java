package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import land.plainfunctional.Immutable;

@Immutable
public class Person extends AbstractRandomIntegerEntity implements Cloneable/*, IMonoid<Person>*/ {

    private static final Person IDENTITY = new Person();

    public static Person getIdentity() {
        return IDENTITY;
    }

    public enum Gender {
        MALE, FEMALE
    }

    // TODO: Create a 'Name' value class
    @Immutable
    public final String name;

    @Immutable
    public final Gender gender;

    @Immutable
    public final LocalDate birthDate;

    public Person() {
        super(null);
        this.name = null;
        this.gender = null;
        this.birthDate = null;
    }

    public Person(String name) {
        super(null);
        this.name = name;
        this.gender = null;
        this.birthDate = null;
    }

    public Person(String name, Gender gender, LocalDate birthDate) {
        super(null);
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public Person(
        String entityId,
        OffsetDateTime entityCreateTime,
        OffsetDateTime entityLastModifyTime,
        String name,
        Gender gender,
        LocalDate birthDate
    ) {
        super(entityId, entityCreateTime, entityLastModifyTime);
        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    /**
     * Just for enabling η-reduction (Lambda calculus: which captures a notion of extensionality).
     */
    public LocalDate getBirthDate() {
        return this.birthDate;
    }

    @Override
    public Person clone() throws CloneNotSupportedException {
        //throw new UnsupportedOperationException();

        //return null;

        //return this;

        return (Person) super.clone();
        //return this.append(IDENTITY);
    }

    // TODO: 'Writer' stuff
    /*
     * Algebraic append<br>
     * <i>Strategy: If the property is missing, add the property of the other ("secondary") address.</i>
     *
     * <p>
     * This binary operation forms a monoid together with this class' 'identity' function
     * (and an enumerated set of {@link Person} objects).
     * </p>
     *
     * @param person The "secondary" person
     * @return the merged/appended/amended person
     /
    @Override
    public Person append(Person person) {
        if (this == IDENTITY) {
            try {
                return person.clone();

            } catch (CloneNotSupportedException exception) {
                System.err.printf("Person::append: Failure when cloning: %s%n", exception);
            }
        }
        if (person == IDENTITY) {
            try {
                return this.clone();

            } catch (CloneNotSupportedException exception) {
                System.err.printf("Person::append: Failure when cloning: %s%n", exception);
            }
        }
        if (!this.entityId.equals(person.entityId)) {
            throw new IllegalArgumentException(format("Appending different entities ('entityId') is not allowed (%s <-> %s)", this.entityId, person.entityId));
        }
        if (!this.entityCreateTime.equals(person.entityCreateTime)) {
            throw new IllegalArgumentException(format("Appending different entities ('entityCreateTime') is not allowed (%s <-> %s)", this.entityId, person.entityId));
        }

        return new Person(
            isNotBlank(this.name) ? this.name : person.name,
            (this.gender != null) ? this.gender : person.gender,
            (this.birthDate != null) ? this.birthDate : person.birthDate
        );
    }

    @Override
    public Person identity() {
        return getIdentity();
    }

    public Person name(String name) {
        return Functions.updateName(this, name);
    }


    ///////////////////////////////////////////////////////
    // Business logic - pure functions
    ///////////////////////////////////////////////////////

    static class Functions {

        static final Function<Person, Function<String, Person>> UPDATE_NAME =
            person ->
                name ->
                    new Person(
                        person.entityId.toString(),
                        person.entityCreateTime,
                        person.entityLastModifyTime,
                        name,
                        person.gender,
                        person.birthDate
                    );

        ///////////////////////////////////////////////////////
        // Extension methods
        ///////////////////////////////////////////////////////

        static Person updateName(Person person, String name) {
            return UPDATE_NAME.apply(person).apply(name);
        }
    }


    ///////////////////////////////////////////////////////
    // Business logic - (side) effects
    ///////////////////////////////////////////////////////

    /
     * (Side) Effects (or effectful/effectual events) are non-pure system actions caused by the pure functions,
     * specified by writer functions (e.g. the one below), and
     * scheduled by writers (e.g. the 'EventSequenceWriter'),
     * executed by writer effect executors (e.g. WriterEventProcessor).
     *
     * These effects are stuff like:
     * - logging
     * - publishing
     * - storing
     * - presentation
     * - delegating (including triggering the next writer functions in the system pipeline)
     *
     * These effects are either synchronous or asynchronous, but mostly the latter, I reckon(/hope)... well
     /
    public static class Effects {

        public static Function<Person, Writer<Person, Effect>> updateName(String newName) {
            return person -> {
                // Domain logic (pure function)
                //Customer updatedCustomer = Functions.SET_ADDRESS.apply(newAddress).apply(customer);
                Person updatedPerson = Functions.updateName(person, newName);
                //Customer updatedCustomer = customer.address(newAddress);

                // Side effect: Logging
                / WORKS
                WriterEvent logEffect = WriterEvent.Builder.use()
                        .type("LOG")
                        .qualifier("DEBUG")
                        .message(format(
                                "Customer.address updated: '%s' => '%s', new customer=[%s]",
                                customer.address.street,
                                updatedCustomer.address.street,
                                updatedCustomer.toValueString()
                        ))
                        .build();
                /
                / WORKS
                WriterEvent logEffect = WriterEvent.Builder.log(
                        "DEBUG",
                        format(
                                "Customer.address updated: '%s' => '%s', new customer=[%s]",
                                customer.address,
                                updatedCustomer.address,
                                updatedCustomer.toValueString()
                        )
                );
                /
                Effect logEffect = Effect.Builder.logTrace(
                    Person.class.getName(),
                    format(
                        "Person.name updated: '%s' => '%s', new person=[%s]",
                        person.name,
                        updatedPerson.name,
                        //updatedPerson.toShortEntityString()
                        updatedPerson
                    )
                );
                // /Logging

                // TODO: Create a Repository<T> interface
                // Side effect: Storing...
                //WriterEvent storingEffect = null; // Illegal!
                Effect storingEffect = Effect.IDENTITY;
                // /Storing

                //return EventSequenceWriter.of(updatedCustomer, logEffect), storingEffect);
                return Writer.of(
                    updatedPerson,
                    Sequence.of(logEffect, storingEffect),
                    (FreeMonoid<Effect>) null,
                    (Effect) null
                );
            };
        }

        public static Function<Person, Writer<Person, BiConsumer<Person, String>>> updateName2(String updatedName) {
            return person -> {

                // Domain logic (pure function)
                Person updatedPerson = Functions.updateName(person, updatedName);

                // Side effect
                BiConsumer<Person, String> effect = new BiConsumer<Person, String>() {
                    @Override
                    public void accept(Person person, String updatedName) {
                        System.out.printf("Doing (side) effect with updatedName=%s (%s)%n", updatedName, person);
                    }
                };

                return Writer.of(
                    updatedPerson,
                    (BiConsumer<Person, String>) effect//,
                    //(FreeMonoid<Consumer<String>>) null,
                    //(Consumer<String>) null
                );
            };
        }

        public static Function<Person, Writer<Person, Pair<BiConsumer<Person, String>, Pair<Person, String>>>> updateName3(String updatedName) {
            return person -> {

                // Domain logic (pure function)
                Person updatedPerson = Functions.updateName(person, updatedName);

                // Side effect
                Pair<BiConsumer<Person, String>, Pair<Person, String>> effect =
                    new ImmutablePair<>(
                        (p, n) -> {
                            System.out.printf("Doing (side) effect with updatedName=%s (%s)%n", n, p);
                            return;
                        },
                        new ImmutablePair<>(person, updatedName)
                    );

                return Writer.of(
                    updatedPerson,
                    effect
                );
            };
        }

        public static Function<Person, Writer2<Person>> updateName4(String updatedName) {
            return person -> {

                // Domain logic (pure function)
                Person updatedPerson = Functions.updateName(person, updatedName);

                // Side effect
                System.out.printf("Doing (side) effect with updatedName=%s (%s)%n", updatedName, person);

                return Writer2.of(updatedPerson);
            };
        }

        public static Function<Person, Maybe<Person>> updateName5(String updatedName) {
            return person -> {

                // Domain logic (pure function)
                Person updatedPerson = Functions.updateName(person, updatedName);

                // Side effect
                System.out.printf("Doing (side) effect with updatedName=%s (%s)%n", updatedName, person);

                return Maybe.of(updatedPerson);
            };
        }

        public static Function<Person, Reader<Maybe<Person>>> updateName6(String updatedName) {
            return person -> {

                // Domain logic (pure function)
                Person updatedPerson = Functions.updateName(person, updatedName);

                // Side effect
                System.out.printf("Doing (side) effect with updatedName=%s (%s)%n", updatedName, person);

                return Reader.of(() -> Maybe.of(updatedPerson));
            };
        }

        public static Function<Person, Writer3<Person>> updateName7(String updatedName) {
            return person -> {

                // Domain logic (pure function)
                Person updatedPerson = Functions.updateName(person, updatedName);

                // Side effect
                Reader<Void> effect = Reader.of(
                    () -> {
                        System.out.printf("Doing (side) effect with updatedName=%s (%s)%n", updatedName, person);
                        return null;
                    }
                );

                return Writer3.of(updatedPerson, effect);
            };
        }
    }
    */

    // TODO: More 'Writer' stuff
    /*
    ///////////////////////////////////////////////////////
    // Services ?? No effects...?
    ///////////////////////////////////////////////////////

    /
     * ...
     /
    public static class Services { }


    ///////////////////////////////////////////////////////
    // Effect monoid
    ///////////////////////////////////////////////////////

    public static BinaryOperator<Writer.Effect> EFFECT_BINARY_OPERATION =
        new BinaryOperator<Effect>() {
            @Override
            public Writer.Effect apply(Writer.Effect effect1, Writer.Effect effect2) {
                //throw new UnsupportedOperationException();
                //return null;
                //if (effect1 == null) {
                //    throw new IllegalArgumentException("Effect argument 1 cannot be null");
                //}
                if (effect2 == null) {
                    throw new IllegalArgumentException("Effect argument 2 cannot be null");
                }
                if (effect2 == Writer.Effect.IDENTITY) {
                    return Writer.Effect.IDENTITY;
                }
                switch (effect2.type) {
                    case LOG:
                        switch (effect2.qualifier) {
                            case "FATAL":
                            case "ERROR":
                            case "WARN":
                                System.err.printf("%-29s %5s %s: %s%n", effect2.timestamp, effect2.id, effect2.qualifier, effect2.message);
                                break;
                            default:
                                System.out.printf("%-29s %5s %s: %s%n", effect2.timestamp, effect2.id, effect2.qualifier, effect2.message);
                                break;
                        }
                        break;

                    default:
                        System.err.printf("Effect type '%s' is not yet supported (%s)%n", effect2.type, effect2);
                        break;
                }

                return Writer.Effect.IDENTITY;
            }
        };

    /
    public static class EffectMonoid extends FreeMonoid<Effect> {

        /
         * @param binaryOperation associative and closed binary operation
         * @param identityElement identity element
         /
        public EffectMonoid(
            BinaryOperator<Effect> binaryOperation,
            Effect identityElement
        ) {
            super(binaryOperation, identityElement);
        }
    }
    */
}
