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

package freemarker.ext.xml;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import freemarker.log.Logger;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModel;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template.utility.ClassUtil;

/**
 * <p>A data model adapter for three widespread XML document object model
 * representations: W3C DOM, dom4j, and JDOM. The adapter automatically
 * recognizes the used XML object model and provides a unified interface for it
 * toward the template. The model provides access to all XML InfoSet features
 * of the XML document and includes XPath support if it has access to the XPath-
 * evaluator library Jaxen. The model's philosophy (which closely follows that
 * of XML InfoSet and XPath) is as follows: it always wraps a list of XML nodes
 * (the "nodelist"). The list can be empty, can have a single element, or can
 * have multiple elements. Every operation applied to the model is applied to
 * all nodes in its nodelist. You usually start with a single- element nodelist,
 * usually the root element node or the document node of the XML tree.
 * Additionally, the nodes can contain String objects as a result of certain
 * evaluations (getting the names of elements, values of attributes, etc.)</p>
 * <p><strong>Implementation note:</strong> If you are using W3C DOM documents
 * built by the Crimson XML parser (or you are using the built-in JDK 1.4 XML
 * parser, which is essentially Crimson), make sure you call
 * <tt>setNamespaceAware(true)</tt> on the 
 * <tt>javax.xml.parsers.DocumentBuilderFactory</tt> instance used for document
 * building even when your documents don't use XML namespaces. Failing to do so,
 * you will experience incorrect behavior when using the documents wrapped with
 * this model.</p>
 *
 * @deprecated Use {@link freemarker.ext.dom.NodeModel} instead.
 */
public class NodeListModel
implements
    TemplateHashModel,
    TemplateMethodModel,
    TemplateScalarModel,
    TemplateSequenceModel,
    TemplateNodeModel
{
    private static final Logger LOG = Logger.getLogger("freemarker.xml");
    
    private static final Class DOM_NODE_CLASS = getClass("org.w3c.dom.Node");
    private static final Class DOM4J_NODE_CLASS = getClass("org.dom4j.Node");
    private static final Navigator DOM_NAVIGATOR = getNavigator("Dom");
    private static final Navigator DOM4J_NAVIGATOR = getNavigator("Dom4j");
    private static final Navigator JDOM_NAVIGATOR = getNavigator("Jdom");
    private static volatile boolean useJaxenNamespaces = true;
    
    // The navigator object that implements document model-specific behavior.
    private final Navigator navigator;
    // The contained nodes
    private final List nodes;
    // The namespaces object (potentially shared by multiple models)
    private Namespaces namespaces;

    /**
     * Creates a new NodeListModel, wrapping the passed nodes.
     * @param nodes you can pass it a single XML node from any supported
     * document model, or a Java collection containing any number of nodes.
     * Passing null is prohibited. To create an empty model, pass it an empty
     * collection. If a collection is passed, all passed nodes must belong to
     * the same XML object model, i.e. you can't mix JDOM and dom4j in a single
     * instance of NodeListModel. The model itself doesn't check for this condition,
     * as it can be time consuming, but will throw spurious
     * {@link ClassCastException}s when it encounters mixed objects.
     * @throws IllegalArgumentException if you pass null
     */
    public NodeListModel(Object nodes) {
        Object node = nodes;
        if(nodes instanceof Collection) {
            this.nodes = new ArrayList((Collection)nodes);
            node = this.nodes.isEmpty() ? null : this.nodes.get(0);
        }
        else if(nodes != null) {
            this.nodes = Collections.singletonList(nodes);
        }
        else {
            throw new IllegalArgumentException("nodes == null");
        }
        if(DOM_NODE_CLASS != null && DOM_NODE_CLASS.isInstance(node)) {
            navigator = DOM_NAVIGATOR;
        }
        else if(DOM4J_NODE_CLASS != null && DOM4J_NODE_CLASS.isInstance(node)) {
            navigator = DOM4J_NAVIGATOR;
        }
        else {
            // Assume JDOM
            navigator = JDOM_NAVIGATOR;
        }
        namespaces = createNamespaces();
    }
    
    private Namespaces createNamespaces() {
        if(useJaxenNamespaces) {
            try {
            	return (Namespaces)
            			Class.forName("freemarker.ext.xml._JaxenNamespaces")
            			.newInstance();
            }
            catch(Throwable t) {
                useJaxenNamespaces = false;
            }
        }
        return new Namespaces();
    }

    private NodeListModel(Navigator navigator, List nodes, Namespaces namespaces) {
        this.navigator = navigator;
        this.nodes = nodes;
        this.namespaces = namespaces;
    }

    private NodeListModel deriveModel(List derivedNodes) {
        namespaces.markShared();
        return new NodeListModel(navigator, derivedNodes, namespaces);
    }
    
    /**
     * Returns the number of nodes in this model's nodelist.
     * @see freemarker.template.TemplateSequenceModel#size()
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Evaluates an XPath expression on XML nodes in this model.
     * @param arguments the arguments to the method invocation. Expectes exactly
     * one argument - the XPath expression.
     * @return a new NodeListModel with nodes selected by applying the XPath
     * expression to this model's nodelist.
     * @see freemarker.template.TemplateMethodModel#exec(List)
     */
    public Object exec(List arguments) throws TemplateModelException {
        if(arguments.size() != 1) {
            throw new TemplateModelException(
                "Expecting exactly one argument - an XPath expression");
        }
        return deriveModel(navigator.applyXPath(nodes, (String)arguments.get(0), namespaces));
    }

    /**
     * Returns the string representation of the wrapped nodes. String objects in
     * the nodelist are rendered as-is (with no XML escaping applied). All other
     * nodes are rendered in the default XML serialization format ("plain XML").
     * This makes the model quite suited for use as an XML-transformation tool.
     * @return the string representation of the wrapped nodes. String objects
     * in the nodelist are rendered as-is (with no XML escaping applied). All
     * other nodes are rendered in the default XML serialization format ("plain
     * XML"). 
     * @see freemarker.template.TemplateScalarModel#getAsString()
     */
    public String getAsString() throws TemplateModelException {
        StringWriter sw = new StringWriter(size() * 128);
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if(o instanceof String) {
                sw.write((String)o);
            }
            else {
                navigator.getAsString(o, sw);
           }
        }
        return sw.toString();
    }

    /**
     * Selects a single node from this model's nodelist by its list index and
     * returns a new NodeListModel containing that single node.
     * @param index the ordinal number of the selected node 
     * @see freemarker.template.TemplateSequenceModel#get(int)
     */
    public TemplateModel get(int index) {
        return deriveModel(Collections.singletonList(nodes.get(index)));
    }

    /**
     * Returns a new NodeListModel containing the nodes that result from applying
     * an operator to this model's nodes.
     * @param key the operator to apply to nodes. Available operators are:
     * <table style="width: auto; border-collapse: collapse" border="1" summary="XML node hash keys">
     *   <thead>
     *     <tr>
     *       <th align="left">Key name</th>
     *       <th align="left">Evaluates to</th>
     *     </tr>  
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <td><tt>*</tt> or <tt>_children</tt></td>
     *       <td>all direct element children of current nodes (non-recursive).
     *         Applicable to element and document nodes.</td>
     *     </tr>  
     *     <tr>
     *       <td><tt>@*</tt> or <tt>_attributes</tt></td>
     *       <td>all attributes of current nodes. Applicable to elements only.
     *         </td>
     *     </tr>
     *     <tr>
     *       <td><tt>@<i>attributeName</i></tt></td>
     *       <td>named attributes of current nodes. Applicable to elements, 
     *         doctypes and processing instructions. On doctypes it supports 
     *         attributes <tt>publicId</tt>, <tt>systemId</tt> and 
     *         <tt>elementName</tt>. On processing instructions, it supports 
     *         attributes <tt>target</tt> and <tt>data</tt>, as well as any 
     *         other attribute name specified in data as 
     *         <tt>name=&quot;value&quot;</tt> pair on dom4j or JDOM models. 
     *         The attribute nodes for doctype and processing instruction are 
     *         synthetic, and as such have no parent. Note, however that 
     *         <tt>@*</tt> does NOT operate on doctypes or processing 
     *         instructions.</td>
     *     </tr>  
     * 
     *     <tr>
     *       <td><tt>_ancestor</tt></td>
     *       <td>all ancestors up to root element (recursive) of current nodes.
     *         Applicable to same node types as <tt>_parent</tt>.</td>
     *     </tr>  
     *     <tr>
     *       <td><tt>_ancestorOrSelf</tt></td>
     *       <td>all ancestors of current nodes plus current nodes. Applicable 
     *         to same node types as <tt>_parent</tt>.</td>
     *     </tr>  
     *     <tr>
     *       <td><tt>_cname</tt></td>
     *       <td>the canonical names of current nodes (namespace URI + local 
     *         name), one string per node (non-recursive). Applicable to 
     *         elements and attributes</td>
     *     </tr>  
     *     <tr>
     *       <td><tt>_content</tt></td>
     *       <td>the complete content of current nodes, including children 
     *         elements, text, entity references, and processing instructions 
     *         (non-recursive). Applicable to elements and documents.</td>
     *     </tr>  
     *     <tr>
     *       <td><tt>_descendant</tt></td>
     *       <td>all recursive descendant element children of current nodes. 
     *         Applicable to document and element nodes.</td>
     *     </tr>  
     *     <tr>
     *       <td><tt>_descendantOrSelf</tt></td>
     *       <td>all recursive descendant element children of current nodes 
     *         plus current nodes. Applicable to document and element nodes.
     *         </td>
     *     </tr>
     *     <tr>
     *       <td><tt>_document</tt></td>
     *       <td>all documents the current nodes belong to. Applicable to all 
     *       nodes except text.</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_doctype</tt></td>
     *       <td>doctypes of the current nodes. Applicable to document nodes 
     *       only.</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_filterType</tt></td>
     *       <td>is a filter-by-type template method model. When called, it 
     *         will yield a node list that contains only those current nodes 
     *         whose type matches one of types passed as argument. You can pass
     *         as many string arguments as you want, each representing one of
     *         the types to select: &quot;attribute&quot;, &quot;cdata&quot;,
     *         &quot;comment&quot;, &quot;document&quot;, 
     *         &quot;documentType&quot;, &quot;element&quot;, 
     *         &quot;entity&quot;, &quot;entityReference&quot;,
     *         &quot;namespace&quot;, &quot;processingInstruction&quot;, or
     *         &quot;text&quot;.</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_name</tt></td>
     *       <td>the names of current nodes, one string per node 
     *         (non-recursive). Applicable to elements and attributes 
     *         (returns their local names), entity references, processing 
     *         instructions (returns its target), doctypes (returns its public
     *         ID)</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_nsprefix</tt></td>
     *       <td>the namespace prefixes of current nodes, one string per node 
     *         (non-recursive). Applicable to elements and attributes</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_nsuri</tt></td>
     *       <td>the namespace URIs of current nodes, one string per node 
     *       (non-recursive). Applicable to elements and attributes</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_parent</tt></td>
     *       <td>parent elements of current nodes. Applicable to element, 
     *       attribute, comment, entity, processing instruction.</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_qname</tt></td>
     *       <td>the qualified names of current nodes in 
     *         <tt>[namespacePrefix:]localName</tt> form, one string per node 
     *         (non-recursive). Applicable to elements and attributes</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_registerNamespace(prefix, uri)</tt></td>
     *       <td>register a XML namespace with the specified prefix and URI for
     *         the current node list and all node lists that are derived from 
     *         the current node list. After registering, you can use the
     *         <tt>nodelist[&quot;prefix:localname&quot;]</tt> or 
     *         <tt>nodelist[&quot;@prefix:localname&quot;]</tt> syntaxes to 
     *         reach elements and attributes whose names are namespace-scoped.
     *         Note that the namespace prefix need not match the actual prefix 
     *         used by the XML document itself since namespaces are compared 
     *         solely by their URI.</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_text</tt></td>
     *       <td>the text of current nodes, one string per node 
     *         (non-recursive). Applicable to elements, attributes, comments, 
     *         processing instructions (returns its data) and CDATA sections. 
     *         The reserved XML characters ('&lt;' and '&amp;') are NOT 
     *         escaped.</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_type</tt></td>
     *       <td>Returns a string describing the type of nodes, one
     *         string per node. The returned values are &quot;attribute&quot;,
     *         &quot;cdata&quot;, &quot;comment&quot;, &quot;document&quot;,
     *         &quot;documentType&quot;, &quot;element&quot;, 
     *         &quot;entity&quot;, &quot;entityReference&quot;, 
     *         &quot;namespace&quot;, &quot;processingInstruction&quot;, 
     *         &quot;text&quot;, or &quot;unknown&quot;.</td>
     *     </tr>
     *     <tr>
     *       <td><tt>_unique</tt></td>
     *       <td>a copy of the current nodes that keeps only the first 
     *         occurrence of every node, eliminating duplicates. Duplicates can
     *         occur in the node list by applying uptree-traversals 
     *         <tt>_parent</tt>, <tt>_ancestor</tt>, <tt>_ancestorOrSelf</tt>,
     *         and <tt>_document</tt> on a node list with multiple elements. 
     *         I.e. <tt>foo._children._parent</tt> will return a node list that
     *         has duplicates of nodes in foo - each node will have the number 
     *         of occurrences equal to the number of its children. In these 
     *         cases, use <tt>foo._children._parent._unique</tt> to eliminate 
     *         duplicates. Applicable to all node types.</td>
     *     </tr>
     *     <tr>
     *       <td>any other key</td>
     *       <td>element children of current nodes with name matching the key. 
     *       This allows for convenience child traversal in 
     *       <tt>book.chapter.title</tt> style syntax. Applicable to document 
     *       and element nodes.</td>
     *     </tr>
     *   </tbody>
     * </table>
     * @return a new NodeListModel containing the nodes that result from applying
     * the operator to this model's nodes.
     * @see freemarker.template.TemplateHashModel#get(String)
     */
    public TemplateModel get(String key) throws TemplateModelException {
        // Try a built-in navigator operator
        NodeOperator op = navigator.getOperator(key);
        String localName = null;
        String namespaceUri = "";
        // If not a nav op, then check for special keys.
        if(op == null && key.length() > 0 && key.charAt(0) == '_') {
            if(key.equals("_unique")) {
                return deriveModel(removeDuplicates(nodes));
            }
            else if(key.equals("_filterType") || key.equals("_ftype")) {
                return new FilterByType();
            }
            else if(key.equals("_registerNamespace")) {
                if(namespaces.isShared()) {
                    namespaces = (Namespaces)namespaces.clone();
                }
            }
        }
        // Last, do a named child element or attribute lookup 
        if(op == null) {
            int colon = key.indexOf(':');
            if(colon == -1) {
                // No namespace prefix specified
                localName = key;
            }
            else {
                // Namespace prefix specified
                localName = key.substring(colon + 1);
                String prefix = key.substring(0, colon);
                namespaceUri = namespaces.translateNamespacePrefixToUri(prefix);
                if(namespaceUri == null) {
                    throw new TemplateModelException("Namespace prefix " + prefix + " is not registered.");
                }
            }
            if(localName.charAt(0) == '@') {
                op = navigator.getAttributeOperator();
                localName = localName.substring(1);
            }
            else {
                op = navigator.getChildrenOperator();
            }
        }
        List result = new ArrayList();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            try {
                op.process(iter.next(), localName, namespaceUri, result);
            }
            catch(RuntimeException e) {
                throw new TemplateModelException(e);
            }
        }
        return deriveModel(result);
    }

    /**
     * Returns true if this NodeListModel contains no nodes. 
     * @see freemarker.template.TemplateHashModel#isEmpty()
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * Registers a namespace prefix-URI pair for subsequent use in {@link
     * #get(String)} as well as for use in XPath expressions.
     * @param prefix the namespace prefix to use for the namespace
     * @param uri the namespace URI that identifies the namespace.
     */
    public void registerNamespace(String prefix, String uri) {
        if(namespaces.isShared()) {
            namespaces = (Namespaces)namespaces.clone();
        }
        namespaces.registerNamespace(prefix, uri);
    }
    
    private class FilterByType
    implements
        TemplateMethodModel
    {
        public Object exec(List arguments)
        {
            List filteredNodes = new ArrayList();
            for (Iterator iter = arguments.iterator(); iter.hasNext();)
            {
                Object node = iter.next();
                if(arguments.contains(navigator.getType(node))) {
                    filteredNodes.add(node);
                }
            }
            return deriveModel(filteredNodes);
        }
    }

    private static final List removeDuplicates(List list)
    {
        int s = list.size();
        ArrayList ulist = new ArrayList(s);
        Set set = new HashSet(s * 4 / 3, .75f);
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (set.add(o)) {
                ulist.add(o);
            }
        }
        return ulist;
    }

    private static Class getClass(String className) {
        try {
            return ClassUtil.forName(className);
        }
        catch(Exception e) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Couldn't load class " + className, e);
            }
            return null;
        }
    }
    
    private static Navigator getNavigator(String navType) {
        try {
            return (Navigator) ClassUtil.forName("freemarker.ext.xml._" + 
                    navType + "Navigator").newInstance();
        }
        catch(Throwable t) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Could not load navigator for " + navType, t);
            }
            return null;
        }
    }

    public TemplateSequenceModel getChildNodes() throws TemplateModelException
    {
        return (TemplateSequenceModel)get("_content");
    }

    public String getNodeName() throws TemplateModelException
    {
        return getUniqueText((NodeListModel)get("_name"), "name");
    }

    public String getNodeNamespace() throws TemplateModelException
    {
        return getUniqueText((NodeListModel)get("_nsuri"), "namespace");
    }

    public String getNodeType() throws TemplateModelException
    {
        return getUniqueText((NodeListModel)get("_type"), "type");
    }
    public TemplateNodeModel getParentNode() throws TemplateModelException
    {
        return (TemplateNodeModel)get("_parent"); 
    }

    private String getUniqueText(NodeListModel model, String property) throws TemplateModelException {
        String s1 = null;
        Set s = null;
        for(Iterator it = model.nodes.iterator(); it.hasNext();) {
            String s2 = (String)it.next();
            if(s2 != null) {
                // No text yet, make this text the current text
                if(s1 == null) {
                    s1 = s2;
                }
                // else if there's already a text and they differ, start 
                // accumulating them for an error message
                else if(!s1.equals(s2)) {
                    if(s == null) {
                        s = new HashSet();
                        s.add(s1);
                    }
                    s.add(s2);
                }
            }
        }
        // If the set for the error messages is empty, return the retval
        if(s == null) {
            return s1;
        }
        // Else throw an exception signaling ambiguity
        throw new TemplateModelException(
            "Value for node " + property + " is ambiguos: " + s);
    }
}
