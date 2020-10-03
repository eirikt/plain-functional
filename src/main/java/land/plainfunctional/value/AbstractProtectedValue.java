package land.plainfunctional.value;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * An abstract class containing a single protected field of type <code>T</code>.
 * Extend this class and overload the constructor for access.<br>
 *
 * <p>...</p>
 *
 * <p>
 * A <i>value object</i> is an object whose identity is solely defined by its state.
 * </p>
 *
 * <p>
 * Value objects are named as so because they are values, and values do not change.
 * If a value changes its value, well, then it is not longer the same value.
 * So, one important design rule for value objects is to make them immutable by default.
 * If one wants to change the state of an existing value object, a new value object should be created.
 * If the application has no more use of the obsolete value object, it should be discarded.
 * </p>
 *
 * <p>
 * Immutable objects are inherently thread-safe.
 * This in contrast to e.g. <i>Entity objects</i>,
 * which should at all time have a final and unique entity id.
 * The entity id defines its identity.
 * Entity objects are mutable by default, as they cannot be discarded/recreated as easily as value objects can.
 * </p>
 *
 * <p>...</p>
 *
 * <p>
 * <i>NB! {@link AbstractProtectedValue} do allow <code>null</code>s.</i>
 * <code>null</code> is a "bottom" (⊥) type/value in Java.
 * It is a subclass of all types, and represents absence of any information.
 * </p>
 *
 * @param <T> The value type
 * @see <a href="https://functionalprogramming.now.sh/1-functions-and-values#values">Values</a>
 * @see <a href="https://en.wikipedia.org/wiki/Bottom_type">Bottom type (Wikipedia)</a>
 */
public abstract class AbstractProtectedValue<T> {

    protected final T value;

    protected AbstractProtectedValue(T value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.value)
            .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        AbstractProtectedValue<?> otherValue = (AbstractProtectedValue<?>) other;

        return new EqualsBuilder()
            .append(this.value, otherValue.value)
            .isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder
            .reflectionToString(this,
                SHORT_PREFIX_STYLE,
                true
            );
    }
}
