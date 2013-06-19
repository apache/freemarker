package freemarker.test.servlet;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class TestSimpleTag2 extends SimpleTagSupport
{
    public void doTag() throws JspException, IOException
    {
        getJspContext().getOut().println("Executed TestSimpleTag2");
    }
}