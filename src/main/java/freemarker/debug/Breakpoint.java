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

package freemarker.debug;

import java.io.Serializable;

/**
 * Represents a breakpoint location consisting of a template name and a line number.
 */
public class Breakpoint implements Serializable, Comparable
{
    private static final long serialVersionUID = 1L;

    private final String templateName;
    private final int line;
    
    /**
     * Creates a new breakpoint.
     * @param templateName the name of the template
     * @param line the line number in the template where to put the breakpoint
     */
    public Breakpoint(String templateName, int line)
    {
        this.templateName = templateName;
        this.line = line;
    }

    /**
     * Returns the line number of the breakpoint
     */
    public int getLine()
    {
        return line;
    }
    /**
     * Returns the template name of the breakpoint
     */
    public String getTemplateName()
    {
        return templateName;
    }

    public int hashCode()
    {
        return templateName.hashCode() + 31 * line;
    }
    
    public boolean equals(Object o)
    {
        if(o instanceof Breakpoint)
        {
            Breakpoint b = (Breakpoint)o;
            return b.templateName.equals(templateName) && b.line == line;
        }
        return false;
    }
    
    public int compareTo(Object o)
    {
        Breakpoint b = (Breakpoint)o;
        int r = templateName.compareTo(b.templateName);
        return r == 0 ? line - b.line : r;
    }
    
    /**
     * Returns the template name and the line number separated with a colon
     */
    public String getLocationString()
    {
        return templateName + ":" + line;
    }
}
