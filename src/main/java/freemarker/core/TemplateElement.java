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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreeNode;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.Collections12;

/**
 * Objects that represent elements in the compiled 
 * tree representation of the template necessarily 
 * descend from this abstract class.
 */
abstract public class TemplateElement extends TemplateObject implements TreeNode {

    TemplateElement parent;

    // Only one of nestedBlock and nestedElements can be non-null.
    TemplateElement nestedBlock;
    List nestedElements; 

    /**
     * Processes the contents of this <tt>TemplateElement</tt> and
     * outputs the resulting text
     *
     * @param env The runtime environment
     */
    abstract void accept(Environment env) throws TemplateException, IOException;

    /**
     * One-line description of the element, that contain all the information that is used in
     * {@link #getCanonicalForm()}, except the nested content (elements) of the element. The expressions inside the 
     * element (the parameters) has to be shown. Meant to be used for stack traces, also for tree views that don't go
     * down to the expression-level. There are no backward-compatibility guarantees regarding the format used ATM, but
     * it must be regular enough to be machine-parseable, and it must contain all information necessary for restoring an
     * AST equivalent to the original.
     * 
     * This final implementation calls {@link #dump(boolean) dump(false)}.
     * 
     * @see #getCanonicalForm()
     * @see #getNodeTypeSymbol()
     */
    public final String getDescription() {
        return dump(false);
    }
    
    /**
     * This final implementation calls {@link #dump(boolean) dump(false)}.
     */
    public final String getCanonicalForm() {
        return dump(true);
    }
    
    /**
     * Tells if the element should show up in error stack traces. If you think you need to set this to {@code false} for
     * an element, always consider if you should use {@link Environment#visitByHiddingParent(TemplateElement)} instead.
     * 
     * Note that this will be ignored for the top (current) element of a stack trace, as that's always shown.
     */
    boolean isShownInStackTrace() {
        return true;
    }

    /**
     * Brings the implementation of {@link #getCanonicalForm()} and {@link #getDescription()} to a single place.
     * Don't call those methods in method on {@code this}, because that will result in infinite recursion! 
     * 
     * @param canonical if {@code true}, it calculates the return value of {@link #getCanonicalForm()},
     *        otherwise of {@link #getDescription()}.
     */
    abstract protected String dump(boolean canonical);
    
// Methods to implement TemplateNodeModel 

    public TemplateNodeModel getParentNode() {
//        return parent;
         return null;
    }
    
    public String getNodeNamespace() {
        return null;
    }
    
    public String getNodeType() {
        return "element";
    }
    
    public TemplateSequenceModel getChildNodes() {
        if (nestedElements != null) {
            return new SimpleSequence(nestedElements);
        }
        SimpleSequence result = new SimpleSequence();
        if (nestedBlock != null) {
            result.add(nestedBlock);
        } 
        return result;
    }
    
    public String getNodeName() {
        String classname = this.getClass().getName();
        int shortNameOffset = classname.lastIndexOf('.')+1;
        return classname.substring(shortNameOffset);
    }
    
// Methods so that we can implement the Swing TreeNode API.    

    public boolean isLeaf() {
        return nestedBlock == null 
               && (nestedElements == null || nestedElements.isEmpty());
    }

    public boolean getAllowsChildren() {
        return !isLeaf();
    }

    public int getIndex(TreeNode node) {
        if (nestedBlock instanceof MixedContent) {
            return nestedBlock.getIndex(node);
        }
        if (nestedBlock != null) {
            if (node == nestedBlock) {
                return 0;
            }
        }
        else if (nestedElements != null) {
            return nestedElements.indexOf(node);
        }
        return -1;
    }

    public int getChildCount() {
        if (nestedBlock instanceof MixedContent) {
            return nestedBlock.getChildCount();
        }
        if (nestedBlock != null) {
            return 1;
        }
        else if (nestedElements != null) {
            return nestedElements.size();
        }
        return 0;
    }

    public Enumeration children() {
        if (nestedBlock instanceof MixedContent) {
            return nestedBlock.children();
        }
        if (nestedBlock != null) {
            return Collections.enumeration(Collections12.singletonList(nestedBlock));
        }
        else if (nestedElements != null) {
            return Collections.enumeration(nestedElements);
        }
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    public TreeNode getChildAt(int index) {
        if (nestedBlock instanceof MixedContent) {
            return nestedBlock.getChildAt(index);
        }
        if (nestedBlock != null) {
            if (index == 0) {
                return nestedBlock;
            }
            throw new ArrayIndexOutOfBoundsException("invalid index");
        }
        else if (nestedElements != null) {
            return(TreeNode) nestedElements.get(index);
        }
        throw new ArrayIndexOutOfBoundsException("element has no children");
    }

    public void setChildAt(int index, TemplateElement element) {
        if(nestedBlock instanceof MixedContent) {
            nestedBlock.setChildAt(index, element);
        }
        else if(nestedBlock != null) {
            if(index == 0) {
                nestedBlock = element;
                element.parent = this;
            }
            else {
                throw new IndexOutOfBoundsException("invalid index");
            }
        }
        else if(nestedElements != null) {
            nestedElements.set(index, element);
            element.parent = this;
        }
        else {
            throw new IndexOutOfBoundsException("element has no children");
        }
    }
    
    public TreeNode getParent() {
        return parent;
    }

    // Walk the tree and set the parent field in all the nested elements recursively.

    void setParentRecursively(TemplateElement parent) {
        this.parent = parent;
        int nestedSize = nestedElements == null ? 0 : nestedElements.size();
        for (int i = 0; i < nestedSize; i++) {
            ((TemplateElement) nestedElements.get(i)).setParentRecursively(this);
        }
        if (nestedBlock != null) {
            nestedBlock.setParentRecursively(this);
        }
    }

    /**
     * We walk the tree and do some cleanup 
     * @param stripWhitespace whether to clean up superfluous whitespace
     */
    TemplateElement postParseCleanup(boolean stripWhitespace) throws ParseException {
        if (nestedElements != null) {
            for (int i = 0; i < nestedElements.size(); i++) {
                TemplateElement te = (TemplateElement) nestedElements.get(i);
                te = te.postParseCleanup(stripWhitespace);
                nestedElements.set(i, te);
                te.parent = this;
            }
            if (stripWhitespace) {
                for (Iterator it = nestedElements.iterator(); it.hasNext();) {
                    TemplateElement te = (TemplateElement) it.next();
                    if (te.isIgnorable()) {
                        it.remove();
                    }
                }
            }
            if (nestedElements instanceof ArrayList) {
                ((ArrayList) nestedElements).trimToSize();
            }
        }
        if (nestedBlock != null) {
            nestedBlock = nestedBlock.postParseCleanup(stripWhitespace);
            if (nestedBlock.isIgnorable()) {
                nestedBlock = null;
            } else {
                nestedBlock.parent = this;
            }
        }
        return this;
    }

    boolean isIgnorable() {
        return false;
    }

// The following methods exist to support some fancier tree-walking 
// and were introduced to support the whitespace cleanup feature in 2.2

    TemplateElement prevTerminalNode() {
        TemplateElement prev = previousSibling();
        if (prev != null) {
            return prev.getLastLeaf();
        }
        else if (parent != null) {
            return parent.prevTerminalNode();
        }
        return null;
    }

    TemplateElement nextTerminalNode() {
        TemplateElement next = nextSibling();
        if (next != null) {
            return next.getFirstLeaf();
        }
        else if (parent != null) {
            return parent.nextTerminalNode();
        }
        return null;
    }



    TemplateElement previousSibling() {
        if (parent == null) {
            return null;
        }
        List siblings = parent.nestedElements;
        if (siblings == null) {
            return null;
        }
        for (int i = siblings.size() - 1; i>=0; i--) {
            if (siblings.get(i) == this) {
                return(i >0) ? (TemplateElement) siblings.get(i-1) : null;
            }
        }
        return null;
    }

    TemplateElement nextSibling() {
        if (parent == null) {
            return null;
        }
        List siblings = parent.nestedElements;
        if (siblings == null) {
            return null;
        }
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i) == this) {
                return (i+1) < siblings.size() ? (TemplateElement) siblings.get(i+1) : null;
            }
        }
        return null;
    }

    private TemplateElement getFirstChild() {
        if (nestedBlock != null) {
            return nestedBlock;
        }
        if (nestedElements != null && nestedElements.size() >0) {
            return(TemplateElement) nestedElements.get(0);
        }
        return null;
    }

    private TemplateElement getLastChild() {
        if (nestedBlock != null) {
            return nestedBlock;
        }
        if (nestedElements != null && nestedElements.size() >0) {
            return(TemplateElement) nestedElements.get(nestedElements.size() -1);
        }
        return null;
    }


    private TemplateElement getFirstLeaf() {
        TemplateElement te = this;
        while (!te.isLeaf() && !(te instanceof Macro) && !(te instanceof BlockAssignment)) {
             // A macro or macro invocation is treated as a leaf here for special reasons
            te = te.getFirstChild();
        }
        return te;
    }

    private TemplateElement getLastLeaf() {
        TemplateElement te = this;
        while (!te.isLeaf() && !(te instanceof Macro) && !(te instanceof BlockAssignment)) {
            // A macro or macro invocation is treated as a leaf here for special reasons
            te = te.getLastChild();
        }
        return te;
    }

    /**
     * determines whether this element's presence on a line 
     * indicates that we should not strip opening whitespace
     * in the post-parse whitespace gobbling step.
     */
    boolean heedsOpeningWhitespace() {
        return false;
    }

    /**
     * determines whether this element's presence on a line 
     * indicates that we should not strip trailing whitespace
     * in the post-parse whitespace gobbling step.
     */
    boolean heedsTrailingWhitespace() {
        return false;
    }
}
