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

package freemarker.core;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateSequenceModel;

/**
 * <b>Internal API - subject to change:</b> Represent directive call, interpolation, text block, or other such
 * non-expression node in the parsed template. Some information that can be found here can be accessed through the
 * {@link Environment#getCurrentDirectiveCallPlace()}, which a published API, and thus promises backward compatibility.
 * 
 * @deprecated This is an internal FreeMarker API with no backward compatibility guarantees, so you shouldn't depend on
 *             it.
 */
@Deprecated
abstract public class TemplateElement extends TemplateObject {

    private static final int INITIAL_REGULATED_CHILD_BUFFER_CAPACITY = 6;

    // ATTENTION! If you add new fields, update #copyFieldsFrom!

    /**
     * The parent element of this element.
     */
    private TemplateElement parent;

    /**
     * Contains 1 or more nested elements with optional trailing {@code null}-s, or is {@code null} exactly if there are
     * no nested elements. Normally, the {@link #parent} of these is the {@code this}, however, in some exceptional
     * cases it's not so, to avoid copying the whole descendant tree with a different parent (as in the result of
     * {@link Macro#Macro(Macro, Macro.WithArgs)}.
     */
    private TemplateElement[] childBuffer;

    /**
     * Contains the number of elements in the {@link #childBuffer}, not counting the trailing {@code null}-s. If this is
     * 0, then and only then {@link #childBuffer} must be {@code null}.
     */
    private int childCount;

    /**
     * The index of the element in the parent's {@link #childBuffer} array.
     * 
     * @since 2.3.23
     */
    private int index;

    // ATTENTION! If you add new fields, update #copyFieldsFrom too!

    /**
     * Executes this {@link TemplateElement}. Usually should not be called directly, but through
     * {@link Environment#visit(TemplateElement)} or a similar {@link Environment} method.
     *
     * @param env
     *            The runtime environment
     * 
     * @return The template elements to execute (meant to be used for nested elements), or {@code null}. Can have
     *         <em>trailing</em> {@code null}-s (unused buffer capacity). Returning the nested elements instead of
     *         executing them inside this method is a trick used for decreasing stack usage when there's nothing to do
     *         after the children was processed anyway.
     */
    abstract TemplateElement[] accept(Environment env) throws TemplateException, IOException;

    /**
     * One-line description of the element, that contains all the information that is used in
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
    @Override
    public final String getCanonicalForm() {
        return dump(true);
    }

    final String getChildrenCanonicalForm() {
        return getChildrenCanonicalForm(childBuffer);
    }
    
    static String getChildrenCanonicalForm(TemplateElement[] children) {
        if (children == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (TemplateElement child : children) {
            if (child == null) {
                break;
            }
            sb.append(child.getCanonicalForm());
        }
        return sb.toString();
    }

    /**
     * Tells if the element should show up in error stack traces. Note that this will be ignored for the top (current)
     * element of a stack trace, as that's always shown.
     */
    boolean isShownInStackTrace() {
        return false;
    }

    /**
     * Tells if this element possibly executes its nested content for many times. This flag is useful when a template
     * AST is modified for running time limiting (see {@link ThreadInterruptionSupportTemplatePostProcessor}). Elements
     * that use {@link #childBuffer} should not need this, as the insertion of the timeout checks is impossible there,
     * given their rigid nested element schema.
     */
    abstract boolean isNestedBlockRepeater();

    /**
     * Brings the implementation of {@link #getCanonicalForm()} and {@link #getDescription()} to a single place. Don't
     * call those methods in method on {@code this}, because that will result in infinite recursion!
     * 
     * @param canonical
     *            if {@code true}, it calculates the return value of {@link #getCanonicalForm()}, otherwise of
     *            {@link #getDescription()}.
     */
    abstract protected String dump(boolean canonical);

    // Methods to implement TemplateNodeModel

    public TemplateNodeModel getParentNode() {
        // return parent;
        return null;
    }

    public String getNodeNamespace() {
        return null;
    }

    public String getNodeType() {
        return "element";
    }

    public TemplateSequenceModel getChildNodes() {
        if (childBuffer != null) {
            final SimpleSequence seq = new SimpleSequence(childCount);
            for (int i = 0; i < childCount; i++) {
                seq.add(childBuffer[i]);
            }
            return seq;
        } else {
            return new SimpleSequence(0);
        }
    }

    public String getNodeName() {
        String classname = this.getClass().getName();
        int shortNameOffset = classname.lastIndexOf('.') + 1;
        return classname.substring(shortNameOffset);
    }

    // Methods so that we can implement the Swing TreeNode API.

    public boolean isLeaf() {
        return childCount == 0;
    }

    /**
     * @deprecated Meaningless; simply returns if the node currently has any child nodes.
     */
    @Deprecated
    public boolean getAllowsChildren() {
        return !isLeaf();
    }

    public int getIndex(TemplateElement node) {
        for (int i = 0; i < childCount; i++) {
            if (childBuffer[i].equals(node)) {
                return i;
            }
        }
        return -1;
    }

    public int getChildCount() {
        return childCount;
    }

    /**
     * Note: For element with {@code #nestedBlock}, this will hide the {@code #nestedBlock} when that's a
     * {@link MixedContent}.
     */
    public Enumeration children() {
        return childBuffer != null
                ? new _ArrayEnumeration(childBuffer, childCount)
                : Collections.enumeration(Collections.EMPTY_LIST);
    }

    /**
     * @deprecated Internal API - even internally, use {@link #getChild(int)} instead.
     */
    @Deprecated
    public TemplateElement getChildAt(int index) {
        if (childCount == 0) {
            throw new IndexOutOfBoundsException("Template element has no children");
        }
        try {
            return childBuffer[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            // nestedElements was a List earlier, so we emulate the same kind of exception
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + childCount);
        }
    }

    public void setChildAt(int index, TemplateElement element) {
        if (index < childCount && index >= 0) {
            childBuffer[index] = element;
            element.index = index;
            element.parent = this;
        } else {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + childCount);
        }
    }

    /**
     * The element whose child this element is, or {@code null} if this is the root node.
     * 
     * @deprecated Don't use in internal code either; use {@link #getParentElement()} there.
     */
    @Deprecated
    public TemplateElement getParent() {
        return parent;
    }
    
    /**
     * The element whose child this element is, or {@code null} if this is the root node.
     */
    final TemplateElement getParentElement() {
        return parent;
    }

    final void setChildBufferCapacity(int capacity) {
        int ln = childCount;
        TemplateElement[] newChildBuffer = new TemplateElement[capacity];
        for (int i = 0; i < ln; i++) {
            newChildBuffer[i] = childBuffer[i];
        }
        childBuffer = newChildBuffer;
    }

    /**
     * Inserts a new nested element after the last nested element.
     */
    final void addChild(TemplateElement nestedElement) {
        addChild(childCount, nestedElement);
    }

    /**
     * Inserts a new nested element at the given index, which can also be one higher than the current highest index.
     */
    final void addChild(int index, TemplateElement nestedElement) {
        final int childCount = this.childCount;

        TemplateElement[] childBuffer = this.childBuffer;
        if (childBuffer == null) {
            childBuffer = new TemplateElement[INITIAL_REGULATED_CHILD_BUFFER_CAPACITY];
            this.childBuffer = childBuffer;
        } else if (childCount == childBuffer.length) {
            setChildBufferCapacity(childCount != 0 ? childCount * 2 : 1);
            childBuffer = this.childBuffer;
        }
        // At this point: nestedElements == this.nestedElements, and has sufficient capacity.

        for (int i = childCount; i > index; i--) {
            TemplateElement movedElement = childBuffer[i - 1];
            movedElement.index = i;
            childBuffer[i] = movedElement;
        }
        nestedElement.index = index;
        nestedElement.parent = this;
        childBuffer[index] = nestedElement;
        this.childCount = childCount + 1;
    }

    final TemplateElement getChild(int index) {
        return childBuffer[index];
    }

    /**
     * @return Array containing 1 or more nested elements with optional trailing {@code null}-s, or is {@code null}
     *         exactly if there are no nested elements.
     */
    final TemplateElement[] getChildBuffer() {
        return childBuffer;
    }

    /**
     * @param buffWithCnt Maybe {@code null}
     * 
     * @since 2.3.24
     */
    final void setChildren(TemplateElements buffWithCnt) {
        TemplateElement[] childBuffer = buffWithCnt.getBuffer();
        int childCount = buffWithCnt.getCount();
        for (int i = 0; i < childCount; i++) {
            TemplateElement child = childBuffer[i];
            child.index = i;
            child.parent = this;
        }
        this.childBuffer = childBuffer;
        this.childCount = childCount;
    }

    /**
     * Beware, parent node of child elements won't match this element.
     */
    final void copyFieldsFrom(TemplateElement that) {
        super.copyFieldsFrom(that);
        this.parent = that.parent;
        this.index = that.index;
        this.childBuffer = that.childBuffer;
        this.childCount = that.childCount;
    }

    final int getIndex() {
        return index;
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
     * Walk the AST subtree rooted by this element, and do simplifications where possible, also removes superfluous
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
        int childCount = this.childCount;
        if (childCount != 0) {
            for (int i = 0; i < childCount; i++) {
                TemplateElement te = childBuffer[i];
                
                /*
                // Assertion:
                if (te.getIndex() != i) {
                    throw new BugException("Invalid index " + te.getIndex() + " (expected: "
                            + i + ") for: " + te.dump(false));
                }
                if (te.getParent() != this) {
                    throw new BugException("Invalid parent " + te.getParent() + " (expected: "
                            + this.dump(false) + ") for: " + te.dump(false));
                }
                */
                
                te = te.postParseCleanup(stripWhitespace);
                childBuffer[i] = te;
                te.parent = this;
                te.index = i;
            }
            for (int i = 0; i < childCount; i++) {
                TemplateElement te = childBuffer[i];
                if (te.isIgnorable(stripWhitespace)) {
                    childCount--;
                    // As later isIgnorable calls might investigates the siblings, we have to move all the items now. 
                    for (int j = i; j < childCount; j++) {
                        final TemplateElement te2 = childBuffer[j + 1];
                        childBuffer[j] = te2;
                        te2.index = j;
                    }
                    childBuffer[childCount] = null;
                    this.childCount = childCount;
                    i--;
                }
            }
            if (childCount == 0) {
                childBuffer = null;
            } else if (childCount < childBuffer.length
                    && childCount <= childBuffer.length * 3 / 4) {
                TemplateElement[] trimmedChildBuffer = new TemplateElement[childCount];
                for (int i = 0; i < childCount; i++) {
                    trimmedChildBuffer[i] = childBuffer[i];
                }
                childBuffer = trimmedChildBuffer;
            }
        }
        return this;
    }

    boolean isIgnorable(boolean stripWhitespace) {
        return false;
    }

    // The following methods exist to support some fancier tree-walking
    // and were introduced to support the whitespace cleanup feature in 2.2

    TemplateElement prevTerminalNode() {
        TemplateElement prev = previousSibling();
        if (prev != null) {
            return prev.getLastLeaf();
        } else if (parent != null) {
            return parent.prevTerminalNode();
        }
        return null;
    }

    TemplateElement nextTerminalNode() {
        TemplateElement next = nextSibling();
        if (next != null) {
            return next.getFirstLeaf();
        } else if (parent != null) {
            return parent.nextTerminalNode();
        }
        return null;
    }

    TemplateElement previousSibling() {
        if (parent == null) {
            return null;
        }
        return index > 0 ? parent.childBuffer[index - 1] : null;
    }

    TemplateElement nextSibling() {
        if (parent == null) {
            return null;
        }
        return index + 1 < parent.childCount ? parent.childBuffer[index + 1] : null;
    }

    private TemplateElement getFirstChild() {
        return childCount == 0 ? null : childBuffer[0];
    }

    private TemplateElement getLastChild() {
        final int childCount = this.childCount;
        return childCount == 0 ? null : childBuffer[childCount - 1];
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

    boolean isChildrenOutputCacheable() {
        int ln = childCount;
        for (int i = 0; i < ln; i++) {
            if (!childBuffer[i].isOutputCacheable()) {
                return false;
            }
        }
        return true;
    }

    /**
     * determines whether this element's presence on a line indicates that we should not strip opening whitespace in the
     * post-parse whitespace gobbling step.
     */
    boolean heedsOpeningWhitespace() {
        return false;
    }

    /**
     * determines whether this element's presence on a line indicates that we should not strip trailing whitespace in
     * the post-parse whitespace gobbling step.
     */
    boolean heedsTrailingWhitespace() {
        return false;
    }
}
