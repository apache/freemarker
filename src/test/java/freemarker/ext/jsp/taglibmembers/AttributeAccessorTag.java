package freemarker.ext.jsp.taglibmembers;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import freemarker.template.utility.StringUtil;

public abstract class AttributeAccessorTag extends SimpleTagSupport {

    protected String name;
    protected String scope;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @SuppressWarnings("boxing")
    protected Integer getScopeAsInteger() {
        if (scope == null) {
            return null;
        }
        if (scope.equals("page")) {
            return PageContext.PAGE_SCOPE;
        }
        if (scope.equals("request")) {
            return PageContext.REQUEST_SCOPE;
        }
        if (scope.equals("session")) {
            return PageContext.SESSION_SCOPE;
        }
        if (scope.equals("application")) {
            return PageContext.APPLICATION_SCOPE;
        }
        throw new IllegalArgumentException("Invalid scope name: " + StringUtil.jQuote(scope));
    }
    
}
