package freemarker.test.servlet;

import java.io.IOException;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class TestSimpleTag extends SimpleTagSupport
{
    private int bodyLoopCount = 1;
    private String name;
    
    public void setName(String name)
    {
        this.name = name;
    }
    
    public void setBodyLoopCount(int bodyLoopCount)
    {
        this.bodyLoopCount = bodyLoopCount;
    }
    
    public void doTag() throws JspException, IOException
    {
        JspContext ctx = getJspContext();
        JspWriter w = ctx.getOut();
        w.println("enter TestSimpleTag " + name);
        JspFragment f = getJspBody();
        for(int i = 0; i < bodyLoopCount; ++i)
        {
            w.println("invoking body i=" + i);
            f.invoke(w);
        }
        w.println("exit TestSimpleTag " + name);
    }
}