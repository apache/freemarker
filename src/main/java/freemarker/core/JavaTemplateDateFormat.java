package freemarker.core;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

class JavaTemplateDateFormat implements TemplateDateFormat {

    private final DateFormat javaDateFormat;

    public JavaTemplateDateFormat(DateFormat javaDateFormat) {
        this.javaDateFormat = javaDateFormat;
    }

    public String format(Date date) {
        return javaDateFormat.format(date);
    }

    public Date parse(String s) throws ParseException {
        return javaDateFormat.parse(s);
    }

    public String getDescription() {
        return javaDateFormat instanceof SimpleDateFormat
                ? ((SimpleDateFormat) javaDateFormat).toPattern()
                : javaDateFormat.toString();
    }

}
