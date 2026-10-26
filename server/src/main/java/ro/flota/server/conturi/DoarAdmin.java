package ro.flota.server.conturi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** O metodă de controller doar pentru ADMIN. Interceptorul o citește la rulare și dă 403 celorlalți. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DoarAdmin {
}
