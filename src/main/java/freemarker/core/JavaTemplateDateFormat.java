package freemarker.core;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;

/**
 * Java {@link DateFormat}-based format.
 */
class JavaTemplateDateFormat extends TemplateDateFormat {
    
    private final DateFormat javaDateFormat;

    public JavaTemplateDateFormat(DateFormat javaDateFormat) {
        this.javaDateFormat = javaDateFormat;
    }
    
    public String format(TemplateDateModel dateModel, boolean zonelessInput) throws TemplateModelException {
        return javaDateFormat.format(dateModel.getAsDate());
    }

    public Date parse(String s) throws ParseException {
        return javaDateFormat.parse(s);
    }

    public String getDescription() {
        return javaDateFormat instanceof SimpleDateFormat
                ? ((SimpleDateFormat) javaDateFormat).toPattern()
                : javaDateFormat.toString();
    }

    public boolean isLocaleBound() {
        return true;
    }

}
