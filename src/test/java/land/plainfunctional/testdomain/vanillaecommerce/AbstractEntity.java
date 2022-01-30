package land.plainfunctional.testdomain.vanillaecommerce;

import java.time.OffsetDateTime;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import land.plainfunctional.Immutable;

/**
 * Abstract class for <i>entity objects</i>,
 * providing consistent <code>hashCode</code> and <code>equals</code> methods based on the entity id.
 * A reflection-based <code>toString</code> methods listing all state, is also included.
 */
@Immutable
public abstract class AbstractEntity implements Entity {

    @Immutable
    protected final OffsetDateTime entityCreateTime;

    @Immutable
    protected final OffsetDateTime entityLastModifyTime;

    protected AbstractEntity() {
        this.entityCreateTime = OffsetDateTime.now();
        this.entityLastModifyTime = this.entityCreateTime;
    }

    protected AbstractEntity(OffsetDateTime entityCreateTime, OffsetDateTime entityLastModifyTime) {
        this.entityCreateTime = entityCreateTime;
        this.entityLastModifyTime = entityLastModifyTime;
    }

    @Override
    public OffsetDateTime entityCreateTime() {
        return this.entityCreateTime;
    }

    @Override
    public OffsetDateTime entityLastModifyTime() {
        return this.entityLastModifyTime;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.entityId())
            .toHashCode();
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (otherObject == null || getClass() != otherObject.getClass()) {
            return false;
        }
        Entity otherEntity = (Entity) otherObject;

        return new EqualsBuilder()
            .append(this.entityId(), otherEntity.entityId())
            .isEquals();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(
            this,
            ToStringStyle.SHORT_PREFIX_STYLE,
            true,
            false,
            true,
            Object.class
        );
    }
}
