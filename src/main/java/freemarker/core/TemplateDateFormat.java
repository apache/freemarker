package freemarker.core;

import java.text.DateFormat;
import java.util.Date;

/**
 * Used for formatting and parsing dates in templates.
 * This is similar to a {@link DateFormat}, but made to fit the requirements of FreeMarker.
 */
interface TemplateDateFormat {
    
    String format(Date date);
    
    Date parse(String s) throws java.text.ParseException;

    /**
     * Meant to be used in error messages to tell what format the parsed string didn't fit.
     */
    String getDescription();

}
