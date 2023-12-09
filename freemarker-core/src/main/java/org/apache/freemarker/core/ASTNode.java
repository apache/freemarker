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

package org.apache.freemarker.core;

/**
 * Superclass of all AST (Abstract Syntax Tree) nodes.
 * <p>
 * The AST is a tree data structure that represent the complete content of a template (static content, directive calls,
 * interpolations and the expressions inside them, possibly comments as well), without the complexities of syntactical
 * details. The AST is generated from the source code (which is text) by the parser of the {@link TemplateLanguage},
 * and focuses on the meaning of the template. Thus, if the same template is rewritten in different template languages
 * (like F3AH is converted to F3SH), the resulting AST-s will remain practically identical.
 * <p>
 * When a {@link Template} is processed, FreeMarker executes the AST directly. (In theory the AST could be translated
 * further to byte code, FreeMarker doesn't try to do that, at least currently.)
 * <p>
 * The AST can also be used to analyze the content of templates, such as to discover its dependencies (on data-model
 * variables, on other templates). 
 */
//TODO [FM3] will be public
abstract class ASTNode {
    
    private Template template;
    int beginColumn, beginLine, endColumn, endLine;
    
    /** This is needed for an ?eval hack; the expression AST nodes will be the descendants of the template, however,
     *  we can't give their position in the template, only in the dynamic string that's evaluated. That's signaled
     *  by a negative line numbers, starting from this constant as line 1. */
    static final int RUNTIME_EVAL_LINE_DISPLACEMENT = -1000000000;  

    final void setLocation(Template template, Token begin, Token end) {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }

    final void setLocation(Template template, Token tagBegin, Token tagEnd, TemplateElements children) {
        ASTElement lastChild = children.getLast();
        if (lastChild != null) {
            // [<#if exp>children]<#else>
            setLocation(template, tagBegin, lastChild);
        } else {
            // [<#if exp>]<#else>
            setLocation(template, tagBegin, tagEnd);
        }
    }
    
    final void setLocation(Template template, Token begin, ASTNode end) {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }
    
    final void setLocation(Template template, ASTNode begin, Token end) {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }

    final void setLocation(Template template, ASTNode begin, ASTNode end) {
        setLocation(template, begin.beginColumn, begin.beginLine, end.endColumn, end.endLine);
    }

    void setLocation(Template template, int beginColumn, int beginLine, int endColumn, int endLine) {
        this.template = template;
        this.beginColumn = beginColumn;
        this.beginLine = beginLine;
        this.endColumn = endColumn;
        this.endLine = endLine;
    }
    
    // Package visible constructor to prevent extending this class outside FreeMarker 
    ASTNode() { }
    
    /**
     * The template that contains this node.
     */
    public Template getTemplate() {
        return template;
    }
    
    /**
     * 1-based column number of the last character of this node in the source code. 0 if not available.
     */
    public final int getBeginColumn() {
        return beginColumn;
    }

    /**
     * 1-based line number of the first character of this node in the source code. 0 if not available.
     */
    // TODO [FM3] No negative number hack in ?eval and such.
    public final int getBeginLine() {
        return beginLine;
    }

    /**
     * 1-based column number of the first character of this node in the source code. 0 if not available.
     */
    public final int getEndColumn() {
        return endColumn;
    }

    /**
     * 1-based line number of the last character of this node in the source code. 0 if not available.
     */
    public final int getEndLine() {
        return endLine;
    }

    final String getSource() {
        String s;
        if (template != null) {
            s = template.getSource(beginColumn, beginLine, endColumn, endLine);
        } else {
            s = null;
        }

        // Can't just return null for backward-compatibility... 
        return s != null ? s : getCanonicalForm();
    }

    @Override
    public final String toString() {
        String s;
    	return (s = getSource()) != null ? s : getCanonicalForm();
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

    ASTNode copyLocationFrom(ASTNode from) {
        template = from.template;
        beginColumn = from.beginColumn;
        beginLine = from.beginLine;
        endColumn = from.endColumn;
        endLine = from.endLine;
        return this;
    }    

    /**
     * Template source code generated from the AST of this node.
     * When parsed, it should result in a practically identical AST that does the same as the original
     * source, assuming that you turn off automatic white-space removal when parsing the canonical form.
     * 
     * @see ASTElement#getLabelWithParameters()
     * @see #getLabelWithoutParameters()
     */
    // TODO [FM3] The whitespace problem isn't OK; do pretty-formatting, outside core if too big.
    abstract public String getCanonicalForm();
    
    /**
     * A very sort single-line string that describes what kind of AST node this is, without describing any 
     * embedded expression or child element. Examples: {@code "#if"}, {@code "+"}, <code>"${...}</code>. These values should
     * be suitable as tree node labels in a tree view. Yet, they should be consistent and complete enough so that an AST
     * that is equivalent with the original could be reconstructed from the tree view. Thus, for literal values that are
     * leaf nodes the symbols should be the canonical form of value.
     *
     * @see #getCanonicalForm()
     * @see ASTElement#getLabelWithParameters()
     */
    public abstract String getLabelWithoutParameters();
    
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
     * LHO {@link ASTExpression} at index 0, and and RHO {@link ASTExpression} at index 1. Or, the binary "." operator will
     * have an LHO {@link ASTExpression} at index 0, and an RHO {@link String}(!) at index 1. Or, the {@code #include}
     * directive will have a path {@link ASTExpression} at index 0, a "parse" {@link ASTExpression} at index 1, etc.
     * 
     * <p>The index value doesn't correspond to the source-code location in general. It's an arbitrary identifier
     * that corresponds to the role of the parameter instead. This also means that when a parameter is omitted, the
     * index of the other parameters won't shift.
     *
     *  @return {@code null} or any kind of {@link Object}, very often an {@link ASTExpression}. However, if there's
     *      a {@link ASTNode} stored inside the returned value, it must itself be be a {@link ASTNode}
     *      too, otherwise the AST couldn't be (easily) fully traversed. That is, non-{@link ASTNode} values
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
