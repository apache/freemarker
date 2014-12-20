package freemarker.ext.jsp.taglibmembers;

import java.io.IOException;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

public class AttributeInfoTag extends AttributeAccessorTag {

    @SuppressWarnings("boxing")
    @Override
    public void doTag() throws JspException, IOException {
        JspContext ctx = getJspContext();
        JspWriter out = ctx.getOut();
        final Integer scope = getScopeAsInteger();
        Object attrVal = scope == null ? ctx.getAttribute(name) : ctx.getAttribute(name, scope);
        out.write(String.valueOf(attrVal));
        if (attrVal != null) {
            out.write(" [");
            out.write(attrVal.getClass().getName());
            out.write("]");
        }
    }

}
