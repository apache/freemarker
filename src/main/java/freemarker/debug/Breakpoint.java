/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.debug;

import java.io.Serializable;

/**
 * Represents a breakpoint location consisting of a template name and a line number.
 * @author Attila Szegedi
 */
public class Breakpoint implements Serializable, Comparable
{
    private static final long serialVersionUID = 1L;

    private final String templateName;
    private final int line;
    private final Serializable data;

    public Breakpoint(String templateName, int line) {
        this(templateName, line, null);
    }
    
    /**
     * Creates a new breakpoint.
     * @param templateName the name of the template
     * @param line the line number in the template where to put the breakpoint
     * @param data arbitrary data attached to this breakpoint, that the debugger client will get back with the
     *        suspension event. As this data will travel back and forth between the client and server, <b>it has to be
     *        de-serializable on the server</b>. Also, it shouldn't be very big serialized, as it will be possibly sent
     *        through network. So if the data is big or might not be de-serializable on another JVM (missing classes), 
     *        the client should just use an identifier as data (like a {@link Long}) with which later it can get
     *        the actual data that's only stored on the client.
     */
    public Breakpoint(String templateName, int line, Serializable data)
    {
        this.templateName = templateName;
        this.line = line;
        this.data = data;
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

    /**
     * Returns the arbitrary data attached to this breakpoint.
     */
    public Serializable getData()
    {
        return data;
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
