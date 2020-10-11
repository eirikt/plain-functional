package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.OffsetDateTime;

/**
 * Marker interface for <i>entities</i>.
 *
 * <p>
 * An entity object should at all time have a final and unique entity id.
 * <i>The entity id defines its identity</i>.
 * </p>
 *
 * <p>
 * Entity objects are mutable by default, as they cannot be discarded as easily as {@link Value} objects can.
 * </p>
 *
 * <p>
 * Typical examples of entities are customers, accounts, and order lines.
 * </p>
 */
public interface Entity {

    /**
     * @return the entity id
     */
    String entityId();


    ///////////////////////////////////////////////////////////////////////////
    // Meta data
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @return the time of the creation of this entity
     */
    OffsetDateTime entityCreateTime();

    /**
     * @return the time of the last modification of this entity
     */
    OffsetDateTime entityLastModifyTime();

    /**
     * @return <code>true</code> if this entity is modified (after its creation)
     */
    boolean isModified();

    /**
     * @return the time of destruction, or <code>null</code> if still an active/valid entity
     */
    OffsetDateTime entityDestroyTime();

    /**
     * @return <code>true</code> if this entity is marked as "destroyed"/archived/deactivated/invalidated and should not be used anymore
     */
    boolean isDestroyed();
}
