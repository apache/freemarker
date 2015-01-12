package freemarker.ext.jsp.taglibmembers;

import java.io.IOException;
import java.util.Date;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import freemarker.template.utility.DateUtil;
import freemarker.template.utility.DateUtil.TrivialDateToISO8601CalendarFactory;

public class AttributeInfoTag extends AttributeAccessorTag {

    @SuppressWarnings("boxing")
    @Override
    public void doTag() throws JspException, IOException {
        JspContext ctx = getJspContext();
        JspWriter out = ctx.getOut();
        final Integer scope = getScopeAsInteger();
        Object attrVal = scope == null ? ctx.getAttribute(name) : ctx.getAttribute(name, scope);
        
        final String formattedVal;
        if (attrVal instanceof Date) {
            formattedVal = DateUtil.dateToISO8601String((Date) attrVal, true, true, true, DateUtil.ACCURACY_SECONDS,
                    DateUtil.UTC,
                    new TrivialDateToISO8601CalendarFactory());
        } else {
            formattedVal = String.valueOf(attrVal);
        }
        
        out.write(formattedVal);
        if (attrVal != null) {
            out.write(" [");
            out.write(attrVal.getClass().getName());
            out.write("]");
        }
    }

}
