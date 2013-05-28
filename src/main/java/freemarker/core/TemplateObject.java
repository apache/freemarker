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

package freemarker.core;

import freemarker.template.*;

/**
 * Objects that represent instructions or expressions
 * in the compiled tree representation of the template
 * all descend from this abstract base class.
 */
public abstract class TemplateObject {

    private Template template;
    int beginColumn, beginLine, endColumn, endLine;

    final void setLocation(Template template, Token begin, Token end)
    throws
        ParseException
    {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }

    final void setLocation(Template template, Token begin, TemplateObject end)
    throws
        ParseException
    {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }

    final void setLocation(Template template, TemplateObject begin, Token end)
    throws
        ParseException
    {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }

    final void setLocation(Template template, TemplateObject begin, TemplateObject end)
    throws
        ParseException
    {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }

    public final int getBeginColumn() {
        return beginColumn;
    }

    public final int getBeginLine() {
        return beginLine;
    }

    public final int getEndColumn() {
        return endColumn;
    }

    public final int getEndLine() {
        return endLine;
    }

    void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine)
    throws
        ParseException
    {
        this.template = template;
        this.beginColumn = beginColumn;
        this.beginLine = beginLine;
        this.endColumn = endColumn;
        this.endLine = endLine;
    }
    
    static void assertNonNull(TemplateModel model, Expression exp, Environment env) throws InvalidReferenceException {
        if (model == null) {
            throw new InvalidReferenceException(
                "Error " + exp.getStartLocation() + ":\n"
                + "The following has evaluated to null or missing:\n" + exp
                + "\n(Hint: If the failing variable is known to be legally null/missing, either specify a default value"
                + " with myOptionalVar!myDefault, or use "
                + MessageUtil.encloseAsTag(exp, "#if myOptionalVar??", false) + "when-present"
                + MessageUtil.encloseAsTag(exp, "#else", false) + "when-missing"
                + MessageUtil.encloseAsTag(exp, "#if", true) + ".)",
                env);
        }
    }

    static TemplateException invalidTypeException(TemplateModel model, Expression exp, Environment env, String expected)
    throws TemplateException {
        return invalidTypeException(model, exp, env, expected, null);
    }
    
    static TemplateException invalidTypeException(
            TemplateModel model, Expression exp, Environment env, String expected,
            String hint)
    throws
        TemplateException
    {
        assertNonNull(model, exp, env);
        return new TemplateException(
            "Error " + exp.getStartLocation() + ":\n"
            + "Expected a value of type " + expected + ", but this evaluated to a value of type " 
            + MessageUtil.getFTLTypeName(model) + ":\n"
            + exp
            + (hint == null ? "" : "\n(Hint: " + hint + ")"),
            env);
    }
    
    /**
     * Returns a string that indicates
     * where in the template source, this object is.
     */
    public String getStartLocation() {
        return MessageUtil.formatLocationForEvaluationError(template, beginLine, beginColumn);
    }

    /**
     * As of 2.3.20. the same as {@link #getStartLocation}. Meant to be used where there's a risk of XSS
     * when viewing error messages.
     */
    public String getStartLocationQuoted() {
        return getStartLocation();
    }

    public String getEndLocation() {
        return MessageUtil.formatLocationForEvaluationError(template, endLine, endColumn);
    }

    /**
     * As of 2.3.20. the same as {@link #getEndLocation}. Meant to be used where there's a risk of XSS
     * when viewing error messages.
     */
    public String getEndLocationQuoted() {
        return getEndLocation();
    }
    
    public final String getSource() {
        if (template != null) {
            return template.getSource(beginColumn, beginLine, endColumn, endLine);
        } else {
            return getCanonicalForm();
        }
    }

    public String toString() {
    	try {
    		return getSource();
    	} catch (Exception e) { // REVISIT: A bit of a hack? (JR)
    		return getCanonicalForm();
    	}
    }


    /**
     * @return whether the point in the template file specified by the 
     * column and line numbers is contained within this template object.
     */
    public boolean contains(int column, int line) {
        if (line < beginLine || line > endLine) {
            return false;
        }
        if (line == beginLine) {
            if (column < beginColumn) {
                return false;
            }
        }
        if (line == endLine) {
            if (column > endColumn) {
                return false;
            }
        }
        return true;
    }

    public Template getTemplate()
    {
        return template;
    }

    TemplateObject copyLocationFrom(TemplateObject from)
    {
        template = from.template;
        beginColumn = from.beginColumn;
        beginLine = from.beginLine;
        endColumn = from.endColumn;
        endLine = from.endLine;
        return this;
    }    

    abstract public String getCanonicalForm();
}
