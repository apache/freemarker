/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.ext.jsp.taglibmembers;

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