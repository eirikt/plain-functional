package land.plainfunctional.testdomain.vanillaecommerce;

/**
 * Marker interface for <i>values</i>.
 *
 * <p>
 * <i>A value object is an object where its identity is solely defined by its state</i>.
 * Value objects are called so because they are values, and values do not change.
 * If a value changes its value, well, then it is not longer the same value.
 * So, one important design rule for value objects is to make them immutable by default.
 * </p>
 *
 * <p>
 * If one wants to change the state of an existing value object, a new value object should be created.
 * </p>
 *
 * <p>
 * If the application has no more use of the obsolete value object, it should be discarded.
 * </p>
 *
 * <p>
 * Immutable objects are inherently thread-safe.
 *</p>
 *
 * <p>
 * Typical examples of a value objects are numbers and addresses.
 * </p>
 */
public interface Value {}
