/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.ext.jsp.taglibmembers;

import java.io.IOException;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.BodyTag;
import jakarta.servlet.jsp.tagext.BodyTagSupport;
import jakarta.servlet.jsp.tagext.IterationTag;
import jakarta.servlet.jsp.tagext.Tag;
import jakarta.servlet.jsp.tagext.TryCatchFinally;

/**
 */
public class TestTag extends BodyTagSupport implements TryCatchFinally {
    private boolean throwException;
    private int repeatCount;
    
    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }

    @Override
    public int doStartTag() throws JspException {
        try {
            pageContext.getOut().println("doStartTag() called here");
            if (throwException) {
                throw new JspException("throwException==true");
            }
            return repeatCount == 0 ? Tag.SKIP_BODY : BodyTag.EVAL_BODY_BUFFERED;
        } catch (IOException e) {
            throw new JspException(e);
        }
    }

    @Override
    public int doAfterBody() throws JspException {
        try {
            getPreviousOut().println("doAfterBody() called here");
            getBodyContent().writeOut(getPreviousOut());
            getBodyContent().clear();
            return --repeatCount == 0 ? Tag.SKIP_BODY : IterationTag.EVAL_BODY_AGAIN;
        } catch (IOException e) {
            throw new JspException(e);
        }
    }
    
    @Override
    public int doEndTag() throws JspException {
        try {
            pageContext.getOut().println("doEndTag() called here");
            return Tag.EVAL_PAGE;
        } catch (IOException e) {
            throw new JspException(e);
        }
    }
    
    public void doCatch(Throwable t) throws Throwable {
        pageContext.getOut().println("doCatch() called here with " + t.getClass() + ": " + getFirstLine(t.getMessage()));
    }

    public void doFinally() {
        try {
            pageContext.getOut().println("doFinally() called here");
        } catch (IOException e) {
            throw new Error(); // Shouldn't happen
        }
    }
    
    private static final String getFirstLine(String s) {
        int brIdx = s.indexOf('\n');
        if (brIdx == -1) brIdx = s.indexOf('\r');
        return brIdx == -1 ? s : s.substring(0, brIdx);
    }
    
}
