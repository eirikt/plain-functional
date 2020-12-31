package land.plainfunctional.util;

public class ReflectionUtils {

    /**
     * Rather speculatively creates an object/instance of the given type.
     * <b>NB! The given type must have an available empty constructor.</b>
     *
     * @return the type's default instance, or an exception if the type is missing an available empty constructor
     */
    public static <T> T createDefaultInstance(Class<T> type) {
        try {
            return type.newInstance();

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
