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

import freemarker.template.Template;

/**
 * Objects that represent instructions or expressions
 * in the compiled tree representation of the template
 * all descend from this abstract base class.
 */
public abstract class TemplateObject {
    
    private Template template;
    int beginColumn, beginLine, endColumn, endLine;
    
    /** This is needed for an ?eval hack; the expression AST nodes will be the descendants of the template, however,
     *  we can't give their position in the template, only in the dynamic string that's evaluated. That's signaled
     *  by a negative line numbers, starting from this constant as line 1. */
    static final int RUNTIME_EVAL_LINE_DISPLACEMENT = -1000000000;  

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
        String s;
        if (template != null) {
            s = template.getSource(beginColumn, beginLine, endColumn, endLine);
        } else {
            s = null;
        }

        // Can't just return null for backward-compatibility... 
        return s != null ? s : getCanonicalForm();
    }

    public String toString() {
        String s;
    	try {
    		s = getSource();
    	} catch (Exception e) { // REVISIT: A bit of a hack? (JR)
    	    s = null;
    	}
    	return s != null ? s : getCanonicalForm();
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

    /**
     * FTL generated from the AST of the node, which must be parseable to an AST that does the same as the original
     * source, assuming we turn off automatic white-space removal when parsing the canonical form.
     * 
     * @see TemplateElement#getDescription()
     * @see #getNodeTypeSymbol()
     */
    abstract public String getCanonicalForm();
    
    /**
     * A very sort single-line string that describes what kind of AST node this is, without describing any 
     * embedded expression or child element. Examples: {@code "#if"}, {@code "+"}, <tt>"${...}</tt>. These values should
     * be suitable as tree node labels in a tree view. Yet, they should be consistent and complete enough so that an AST
     * that is equivalent with the original could be reconstructed from the tree view. Thus, for literal values that are
     * leaf nodes the symbols should be the canonical form of value.
     * 
     * Note that {@link TemplateElement#getDescription()} has similar role, only it doesn't go under the element level
     * (i.e. down to the expression level), instead it always prints the embedded expressions itself.
     * 
     * @see #getCanonicalForm()
     * @see TemplateElement#getDescription()
     */
    abstract String getNodeTypeSymbol();
    
    /**
     * Returns highest valid parameter index + 1. So one should scan indexes with {@link #getParameterValue(int)}
     * starting from 0 up until but excluding this. For example, for the binary "+" operator this will give 2, so the
     * legal indexes are 0 and 1. Note that if a parameter is optional in a template-object-type and happens to be
     * omitted in an instance, this will still return the same value and the value of that parameter will be
     * {@code null}.
     */
    abstract int getParameterCount();
    
    /**
     * Returns the value of the parameter identified by the index. For example, the binary "+" operator will have an
     * LHO {@link Expression} at index 0, and and RHO {@link Expression} at index 1. Or, the binary "." operator will
     * have an LHO {@link Expression} at index 0, and an RHO {@link String}(!) at index 1. Or, the {@code #include}
     * directive will have a path {@link Expression} at index 0, a "parse" {@link Expression} at index 1, etc.
     * 
     * <p>The index value doesn't correspond to the source-code location in general. It's an arbitrary identifier
     * that corresponds to the role of the parameter instead. This also means that when a parameter is omitted, the
     * index of the other parameters won't shift.
     *
     *  @return {@code null} or any kind of {@link Object}, very often an {@link Expression}. However, if there's
     *      a {@link TemplateObject} stored inside the returned value, it must itself be be a {@link TemplateObject}
     *      too, otherwise the AST couldn't be (easily) fully traversed. That is, non-{@link TemplateObject} values
     *      can only be used for leafs. 
     *  
     *  @throws IndexOutOfBoundsException if {@code idx} is less than 0 or not less than {@link #getParameterCount()}. 
     */
    abstract Object getParameterValue(int idx);

    /**
     *  Returns the role of the parameter at the given index, like {@link ParameterRole#LEFT_HAND_OPERAND}.
     *  
     *  As of this writing (2013-06-17), for directive parameters it will always give {@link ParameterRole#UNKNOWN},
     *  because there was no need to be more specific so far. This should be improved as need.
     *  
     *  @throws IndexOutOfBoundsException if {@code idx} is less than 0 or not less than {@link #getParameterCount()}. 
     */
    abstract ParameterRole getParameterRole(int idx);
    
}
