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

package freemarker.template;

/**
 * "node" template language data type: an object that is a node in a tree.
 * A tree of nodes can be recursively <em>visited</em> using the &lt;#visit...&gt; and &lt;#recurse...&gt;
 * directives. This API is largely based on the W3C Document Object Model
 * (DOM) API. However, it's meant to be generally useful for describing
 * any tree of objects that you wish to navigate using a recursive visitor
 * design pattern (or simply through being able to get the parent
 * and child nodes).
 * 
 * <p>See the <a href="http://freemarker.org/docs/xgui.html" target="_blank">XML
 * Processing Guide</a> for a concrete application.
 *
 * @since FreeMarker 2.3
 * @author <a href="mailto:jon@revusky.com">Jonathan Revusky</a>
 */
public interface TemplateNodeModel extends TemplateModel {
    
    /**
     * @return the parent of this node or null, in which case
     * this node is the root of the tree.
     */
    TemplateNodeModel getParentNode() throws TemplateModelException;
    
    /**
     * @return a sequence containing this node's children.
     * If the returned value is null or empty, this is essentially 
     * a leaf node.
     */
    TemplateSequenceModel getChildNodes() throws TemplateModelException;

    /**
     * @return a String that is used to determine the processing
     * routine to use. In the XML implementation, if the node 
     * is an element, it returns the element's tag name.  If it
     * is an attribute, it returns the attribute's name. It 
     * returns "@text" for text nodes, "@pi" for processing instructions,
     * and so on.
     */    
    String getNodeName() throws TemplateModelException;
    
    /**
     * @return a String describing the <em>type</em> of node this is.
     * In the W3C DOM, this should be "element", "text", "attribute", etc.
     * A TemplateNodeModel implementation that models other kinds of
     * trees could return whatever it appropriate for that application. It
     * can be null, if you don't want to use node-types.
     */
    String getNodeType() throws TemplateModelException;
    
    
    /**
     * @return the XML namespace URI with which this node is 
     * associated. If this TemplateNodeModel implementation is 
     * not XML-related, it will almost certainly be null. Even 
     * for XML nodes, this will often be null.
     */
    String getNodeNamespace() throws TemplateModelException;
}
