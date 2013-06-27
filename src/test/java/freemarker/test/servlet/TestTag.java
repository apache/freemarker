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

package freemarker.test.servlet;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.IterationTag;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TryCatchFinally;

/**
 * @author Attila Szegedi
 */
public class TestTag extends BodyTagSupport implements TryCatchFinally
{
    private boolean throwException;
    private int repeatCount;
    
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    public int doStartTag() throws JspException {
        try {
            pageContext.getOut().println("doStartTag() called here");
            if(throwException) {
                throw new JspException("throwException==true");
            }
            return repeatCount == 0 ? Tag.SKIP_BODY : BodyTag.EVAL_BODY_BUFFERED;
        }
        catch(IOException e) {
            throw new JspException(e);
        }
    }

    public int doAfterBody() throws JspException {
        try {
            getPreviousOut().println("doAfterBody() called here");
            getBodyContent().writeOut(getPreviousOut());
            getBodyContent().clear();
            return --repeatCount == 0 ? Tag.SKIP_BODY : IterationTag.EVAL_BODY_AGAIN;
        }
        catch(IOException e) {
            throw new JspException(e);
        }
    }
    
    public int doEndTag() throws JspException {
        try {
            pageContext.getOut().println("doEndTag() called here");
            return Tag.EVAL_PAGE;
        }
        catch(IOException e) {
            throw new JspException(e);
        }
    }
    
    public void doCatch(Throwable t) throws Throwable {
        pageContext.getOut().println("doCatch() called here with " + t.getClass() + ": " + getFirstLine(t.getMessage()));
    }

    public void doFinally() {
        try {
            pageContext.getOut().println("doFinally() called here");
        }
        catch(IOException e) {
            throw new Error(); // Shouldn't happen
        }
    }
    
    private static final String getFirstLine(String s) {
        int brIdx = s.indexOf('\n');
        if (brIdx == -1) brIdx = s.indexOf('\r');
        return brIdx == -1 ? s : s.substring(0, brIdx);
    }
    
}
