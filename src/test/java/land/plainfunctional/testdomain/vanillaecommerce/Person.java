package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.LocalDate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

// TODO: Make immutable
public class Person extends AbstractRandomIntegerEntity implements Cloneable {

    public static final Person IDENTITY = new Person();

    public static Person identity() {
        return IDENTITY;
    }

    public enum Gender {
        MALE, FEMALE
    }

    public String name; // TODO: Create a 'Name' value class
    public Gender gender;
    public LocalDate birthDate;

    public Person() {
        super(null);
    }

    /**
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
     */
    public Person append(Person person) {
        // TODO: More like this:
        //return new Person(
        //    isNotBlank(this.name) ? this.name : person.name,
        //    (this.gender != null) ? this.gender : person.gender,
        //    (this.birthDate != null) ? this.birthDate : person.birthDate
        //);
        Person mergedPerson = new Person();

        mergedPerson.name = isNotBlank(this.name) ? this.name : person.name;
        mergedPerson.gender = this.gender != null ? this.gender : person.gender;
        mergedPerson.birthDate = this.birthDate != null ? this.birthDate : person.birthDate;

        return mergedPerson;
    }

    @Override
    public Person clone() throws CloneNotSupportedException {
        return (Person) super.clone();
    }
}
