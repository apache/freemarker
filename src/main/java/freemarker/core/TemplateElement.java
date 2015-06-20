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

package freemarker.core;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;

/**
 * <b>Internal API - subject to change:</b> Represent directive call, interpolation, text block, or other such
 * non-expression node in the parsed template. Some information that can be found here can be accessed through the
 * {@link Environment#getCurrentDirectiveCallPlace()}, which a published API, and thus promises backward
 * compatibility.
 * 
 * @deprecated This is an internal FreeMarker API with no backward compatibility guarantees, so you shouldn't depend on
 *             it.
 */
abstract public class TemplateElement extends TemplateObject implements TreeNode {

    private static final int INITIAL_REGULATED_CHILD_BUFFER_CAPACITY = 6;

    private TemplateElement parent;

    /**
     * Used by elements that has no fixed schema for its child elements. For example, a {@code #case} can enclose any
     * kind of elements. Only one of {@link #nestedBlock} and {@link #regulatedChildBuffer} can be non-{@code null}.
     * This element is typically a {@link MixedContent}, at least before {@link #postParseCleanup(boolean)} (which
     * optimizes out {@link MixedContent} with child count less than 2).
     */
    private TemplateElement nestedBlock;
    
    /**
     * Used by elements that has a fixed schema for its child elements. For example, {@code #switch} can only have
     * {@code #case} and {@code #default} child elements. Only one of {@link #nestedBlock} and
     * {@link #regulatedChildBuffer} can be non-{@code null}.
     */
    private TemplateElement[] regulatedChildBuffer;
    private int regulatedChildCount;

    /**
     * The index of the element in the parent's {@link #regulatedChildBuffer} array, or 0 if this is the
     * {@link #nestedBlock} of the parent.
     * 
     * @since 2.3.23
     */
    private int index;

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
     * Tells if this element possibly executes its {@link #nestedBlock} for many times. This flag is useful when
     * a template AST is modified for running time limiting (see {@link ThreadInterruptionSupportTemplatePostProcessor}).
     * Elements that use {@link #regulatedChildBuffer} should not need this, as the insertion of the timeout checks is
     * impossible there, given their rigid nested element schema.
     */
    abstract boolean isNestedBlockRepeater();

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
        if (regulatedChildBuffer != null) {
            final SimpleSequence seq = new SimpleSequence(regulatedChildCount);
            for (int i = 0; i < regulatedChildCount; i++) {
                seq.add(regulatedChildBuffer[i]);
            }
            return seq;
        }
        SimpleSequence result = new SimpleSequence(1);
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
        return nestedBlock == null && regulatedChildCount == 0;
    }

    /**
     * @deprecated Meaningless; simply returns if the node currently has any child nodes.
     */
    public boolean getAllowsChildren() {
        return !isLeaf();
    }

    /**
     * @deprecated Starting from 2.4, we won't use {@link TreeNode} API, as it requires Swing.
     */
    public int getIndex(TreeNode node) {
        if (nestedBlock instanceof MixedContent) {
            return nestedBlock.getIndex(node);
        }
        if (nestedBlock != null) {
            if (node == nestedBlock) {
                return 0;
            }
        } else {
            for (int i = 0; i < regulatedChildCount; i++) {
                if (regulatedChildBuffer[i].equals(node)) { 
                    return i;
                }
            }
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
        return regulatedChildCount;
    }

    /**
     * Note: For element with {@code #nestedBlock}, this will hide the {@code #nestedBlock} when that's a
     * {@link MixedContent}.
     */
    public Enumeration children() {
        if (nestedBlock instanceof MixedContent) {
            return nestedBlock.children();
        }
        if (nestedBlock != null) {
            return Collections.enumeration(Collections.singletonList(nestedBlock));
        }
        else if (regulatedChildBuffer != null) {
            return new _ArrayEnumeration(regulatedChildBuffer, regulatedChildCount);
        }
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     * @deprecated This method will return {@link TemplateElement} starting from 2.4, as that doesn't require Swing;
     * don't use it.
     */
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
        else if (regulatedChildCount != 0) {
            try {
                return regulatedChildBuffer[index];
            } catch (ArrayIndexOutOfBoundsException e) {
                // nestedElements was a List earlier, so we emulate the same kind of exception
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + regulatedChildCount);
            }
        }
        throw new ArrayIndexOutOfBoundsException("Template element has no children");
    }

    public void setChildAt(int index, TemplateElement element) {
        if(nestedBlock instanceof MixedContent) {
            nestedBlock.setChildAt(index, element);
        }
        else if(nestedBlock != null) {
            if(index == 0) {
                nestedBlock = element;
                element.index = 0;
                element.parent = this;
            }
            else {
                throw new IndexOutOfBoundsException("invalid index");
            }
        }
        else if(regulatedChildBuffer != null) {
            regulatedChildBuffer[index] = element;
            element.index = index;
            element.parent = this;
        }
        else {
            throw new IndexOutOfBoundsException("element has no children");
        }
    }
    
    /**
     * @deprecated This method will return {@link TemplateElement} starting from 2.4, as that doesn't require Swing;
     * don't use it.
     */
    public TreeNode getParent() {
        return parent;
    }
    
    final void setRegulatedChildBufferCapacity(int capacity) {
        int ln = regulatedChildCount;
        TemplateElement[] newRegulatedChildBuffer = new TemplateElement[capacity];
        for (int i = 0; i < ln; i++) {
            newRegulatedChildBuffer[i] = regulatedChildBuffer[i];
        }
        regulatedChildBuffer = newRegulatedChildBuffer;
    }
    
    final void addRegulatedChild(TemplateElement nestedElement) {
        addRegulatedChild(regulatedChildCount, nestedElement);
    }

    final void addRegulatedChild(int index, TemplateElement nestedElement) {
        final int lRegulatedChildCount = regulatedChildCount;
        
        TemplateElement[] lRegulatedChildBuffer = regulatedChildBuffer;
        if (lRegulatedChildBuffer == null) {
            lRegulatedChildBuffer = new TemplateElement[INITIAL_REGULATED_CHILD_BUFFER_CAPACITY];
            regulatedChildBuffer = lRegulatedChildBuffer;
        } else if (lRegulatedChildCount == lRegulatedChildBuffer.length) {
            setRegulatedChildBufferCapacity(lRegulatedChildCount != 0 ? lRegulatedChildCount * 2 : 1);
            lRegulatedChildBuffer = regulatedChildBuffer; 
        }
        // At this point: nestedElements == this.nestedElements, and has sufficient capacity.

        for (int i = lRegulatedChildCount; i > index; i--) {
            TemplateElement movedElement = lRegulatedChildBuffer[i - 1];
            movedElement.index = i;
            lRegulatedChildBuffer[i] = movedElement;
        }
        nestedElement.index = index;
        nestedElement.parent = this;
        lRegulatedChildBuffer[index] = nestedElement;
        regulatedChildCount = lRegulatedChildCount + 1;
    }
    
    final int getRegulatedChildCount() {
       return regulatedChildCount; 
    }
    
    final TemplateElement getRegulatedChild(int index) {
        return regulatedChildBuffer[index];
    }
    
    final int getIndex() {
        return index;
    }
    
    /**
     * The element whose child this element is, or {@code null} if this is the root node.
     */
    final TemplateElement getParentElement() {
        return parent;
    }
    
    final TemplateElement getNestedBlock() {
        return nestedBlock;
    }

    final void setNestedBlock(TemplateElement nestedBlock) {
        if (nestedBlock != null) {
            nestedBlock.parent = this;
            nestedBlock.index = 0;
        }
        this.nestedBlock = nestedBlock;
    }
    
    /**
     * This is a special case, because a root element is not contained in another element, so we couldn't set the
     * private fields.
     */
    final void setFieldsForRootElement() {
        index = 0;
        parent = null;
    }

    /**
     * Walk the AST subtree rooted by this element, and do simplifications where possible, also remove superfluous
     * whitespace.
     * 
     * @param stripWhitespace
     *            whether to remove superfluous whitespace
     * 
     * @return The element this element should be replaced with in the parent. If it's the same as this element, no
     *         actual replacement will happen. Note that adjusting the {@link #parent} and {@link #index} of the result
     *         is the duty of the caller, not of this method.
     */
    TemplateElement postParseCleanup(boolean stripWhitespace) throws ParseException {
        int regulatedChildCount = this.regulatedChildCount;
        if (regulatedChildCount != 0) {
            for (int i = 0; i < regulatedChildCount; i++) {
                TemplateElement te = regulatedChildBuffer[i];
                te = te.postParseCleanup(stripWhitespace);
                regulatedChildBuffer[i] = te;
                te.parent = this;
                te.index = i;
            }
            if (stripWhitespace) {
                for (int i = 0; i < regulatedChildCount; i++) {
                    TemplateElement te = regulatedChildBuffer[i];
                    if (te.isIgnorable()) {
                        regulatedChildCount--;
                        for (int j = i; j < regulatedChildCount; j++) {
                            final TemplateElement te2 = regulatedChildBuffer[j  + 1];
                            regulatedChildBuffer[j] = te2;
                            te2.index = j;
                        }
                        regulatedChildBuffer[regulatedChildCount] = null;
                        this.regulatedChildCount = regulatedChildCount;
                        i--;
                    }
                }
            }
            if (regulatedChildCount < regulatedChildBuffer.length
                    && regulatedChildCount <= regulatedChildBuffer.length * 3 / 4) {
                TemplateElement[] trimmedregulatedChildBuffer = new TemplateElement[regulatedChildCount];
                for (int i = 0; i < regulatedChildCount; i++) {
                    trimmedregulatedChildBuffer[i] = regulatedChildBuffer[i];
                }
                regulatedChildBuffer = trimmedregulatedChildBuffer;
            }
        } else if (nestedBlock != null) {
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
        return index > 0 ? parent.regulatedChildBuffer[index - 1] : null;
    }

    TemplateElement nextSibling() {
        if (parent == null) {
            return null;
        }
        return index + 1 < parent.regulatedChildCount ? parent.regulatedChildBuffer[index + 1] : null;
    }

    private TemplateElement getFirstChild() {
        if (nestedBlock != null) {
            return nestedBlock;
        }
        if (regulatedChildCount == 0) {
            return null;
        }
        return regulatedChildBuffer[0];
    }

    private TemplateElement getLastChild() {
        if (nestedBlock != null) {
            return nestedBlock;
        }
        final int regulatedChildCount = this.regulatedChildCount;
        if (regulatedChildCount == 0) {
            return null;
        }
        return regulatedChildBuffer[regulatedChildCount - 1];
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
     * Tells if executing this element has output that only depends on the template content and that has no side
     * effects.
     */
    boolean isOutputCacheable() {
        return false;
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
