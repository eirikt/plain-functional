package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.OffsetDateTime;

import land.plainfunctional.Immutable;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * {@inheritDoc}
 *
 * <p>...</p>
 *
 * <p>
 * <i>
 * <b>NB!</b>
 * The entity id provided by this class is just a random {@link Integer}.
 * This class is suitable for test entities with the need for simple, read-friendly entity ids.<br>
 * <b>Do not use this class in production environments!</b>
 * </i>
 * </p>
 */
@Immutable
public abstract class AbstractRandomIntegerEntity extends AbstractEntity {

    /**
     * An {@link Integer}-based entity id.
     */
    @Immutable
    protected final Integer entityId;

    protected AbstractRandomIntegerEntity(int entityId) {
        super();
        // Known entity id / Existing entity
        this.entityId = entityId;
    }

    protected AbstractRandomIntegerEntity(String entityId) {
        super();
        if (isBlank(entityId)) {
            // New entity
            this.entityId = nextInt();
        } else {
            // Existing entity
            this.entityId = Integer.parseInt(entityId);
        }
    }

    protected AbstractRandomIntegerEntity(String entityId, OffsetDateTime entityCreateTime, OffsetDateTime entityLastModifyTime) {
        super(entityCreateTime, entityLastModifyTime);
        if (isBlank(entityId)) {
            // New entity
            this.entityId = nextInt();
        } else {
            // Existing entity
            this.entityId = Integer.parseInt(entityId);
        }
    }

    @Override
    public String entityId() {
        return this.entityId.toString();
    }
}
