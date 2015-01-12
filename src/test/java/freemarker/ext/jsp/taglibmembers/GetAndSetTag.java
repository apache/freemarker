package freemarker.ext.jsp.taglibmembers;

import java.io.IOException;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import freemarker.template.utility.NullArgumentException;

public class GetAndSetTag extends AttributeAccessorTag {
    
    private Object value;
    
    @SuppressWarnings("boxing")
    @Override
    public void doTag() throws JspException, IOException {
        NullArgumentException.check("name", name);
        Integer scopeInt = getScopeAsInteger();

        JspContext ctx = getJspContext();
        JspWriter out = ctx.getOut();
        
        out.write(scope == null ? "any:" : scope + ":");
        out.write(name);
        out.write(" was ");
        out.write(String.valueOf((scope == null ? ctx.getAttribute(name) : ctx.getAttribute(name, scopeInt))));
        if (scope == null) {
            ctx.setAttribute(name, value);
        } else {
            ctx.setAttribute(name, value, scopeInt);
        }
        out.write(", set to ");
        out.write(String.valueOf(value));
        out.write("\n");
        
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

}
