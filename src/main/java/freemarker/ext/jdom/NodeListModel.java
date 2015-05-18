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

package freemarker.ext.jdom;

import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jaxen.Context;
import org.jaxen.JaxenException;
import org.jaxen.NamespaceContext;
import org.jaxen.jdom.JDOMXPath;
import org.jdom.Attribute;
import org.jdom.CDATA;
import org.jdom.Comment;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.EntityRef;
import org.jdom.Namespace;
import org.jdom.ProcessingInstruction;
import org.jdom.Text;
import org.jdom.output.XMLOutputter;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelIterator;
import freemarker.template.TemplateScalarModel;
import freemarker.template.TemplateSequenceModel;

/**
 * Provides a template for wrapping JDOM objects. It is capable of storing not only
 * a single JDOM node, but a list of JDOM nodes at once (hence the name).
 * Each node is an instance of any of the core JDOM node classes (except namespaces,
 * which are not supported at the moment), or String for representing text.
 * See individual method documentation for exact details on how the class works. In
 * short:
 * <ul>
 * <li>{@link #getAsString()} will render all contained nodes as XML fragment,
 * <li>{@link #exec(List)} provides full XPath functionality implemented on top of
 * the <a href="http://www.jaxen.org">Jaxen</a> library,</li>
 * <li>{@link #get(String)} provides node traversal, copying and filtering - somewhat
 * less expressive than XPath, however it does not require the external library and
 * it evaluates somewhat faster</li>
 * <li>being a {@link TemplateCollectionModel} allows to iterate the contained node list, and</li>
 * <li>being a {@link TemplateSequenceModel} allows to access the contained nodes by index and query the node count.</li>
 * </ul>
 * 
 * <p><b>Note:</b> There is a JDOM independent re-implementation of this class:
 *   {@link freemarker.ext.xml.NodeListModel freemarker.ext.xml.NodeListModel}
 * 
 * @deprecated Use {@link freemarker.ext.dom.NodeModel} instead.
 */
public class NodeListModel
implements
    TemplateHashModel,
    TemplateMethodModel,
    TemplateCollectionModel,
    TemplateSequenceModel,
    TemplateScalarModel
{
    private static final AttributeXMLOutputter OUTPUT = new AttributeXMLOutputter();
    // A convenience singleton for representing a node list without nodes.
    private static final NodeListModel EMPTY = new NodeListModel(null, false);

    // Cache of already parsed XPath expressions
    private static final Map XPATH_CACHE = new WeakHashMap();

    private static final NamedNodeOperator NAMED_CHILDREN_OP = new NamedChildrenOp();
    private static final NamedNodeOperator NAMED_ATTRIBUTE_OP = new NamedAttributeOp();
    private static final NodeOperator ALL_ATTRIBUTES_OP = new AllAttributesOp();
    private static final NodeOperator ALL_CHILDREN_OP = new AllChildrenOp();
    private static final Map OPERATIONS = createOperations();
    private static final Map SPECIAL_OPERATIONS = createSpecialOperations();
    private static final int SPECIAL_OPERATION_COPY = 0;
    private static final int SPECIAL_OPERATION_UNIQUE = 1;
    private static final int SPECIAL_OPERATION_FILTER_NAME = 2;
    private static final int SPECIAL_OPERATION_FILTER_TYPE = 3;
    private static final int SPECIAL_OPERATION_QUERY_TYPE = 4;
    private static final int SPECIAL_OPERATION_REGISTER_NAMESPACE = 5;
    private static final int SPECIAL_OPERATION_PLAINTEXT = 6;

    // The contained nodes
    private final List nodes;
    private final Map namespaces;

    /**
     * Creates a node list that holds a single {@link Document} node.
     */
    public NodeListModel(Document document)
    {
        nodes = document == null ? Collections.EMPTY_LIST : Collections.singletonList(document);
        namespaces = new HashMap();
    }

    /**
     * Creates a node list that holds a single {@link Element} node.
     */
    public NodeListModel(Element element)
    {
        nodes = element == null ? Collections.EMPTY_LIST : Collections.singletonList(element);
        namespaces = new HashMap();
    }

    private NodeListModel(Object object, Map namespaces)
    {
        nodes = object == null ? Collections.EMPTY_LIST : Collections.singletonList(object);
        this.namespaces = namespaces;
    }

    /**
     * Creates a node list that holds a list of nodes.
     * @param nodes the list of nodes this template should hold. The created template
     * will copy the passed nodes list, so changes to the passed list will not affect
     * the model.
     */
    public NodeListModel(List nodes)
    {
        this(nodes, true);
    }

    /**
     * Creates a node list that holds a list of nodes.
     * @param nodes the list of nodes this template should hold.
     * @param copy if true, the created template will copy the passed nodes list,
     * so changes to the passed list will not affect the model. If false, the model
     * will reference the passed list and will sense changes in it, although no
     * operations on the list will be synchronized.
     */
    public NodeListModel(List nodes, boolean copy)
    {
        this.nodes = copy && nodes != null ? new ArrayList(nodes) : (nodes == null ? Collections.EMPTY_LIST : nodes);
        namespaces = new HashMap();
    }

    private NodeListModel(List nodes, Map namespaces)
    {
        this.nodes = nodes == null ? Collections.EMPTY_LIST : nodes;
        this.namespaces = namespaces;
    }

    private static final NodeListModel createNodeListModel(List list, Map namespaces)
    {
        if (list == null || list.isEmpty()) {
            if (namespaces.isEmpty()) {
                return EMPTY;
            } else {
                return new NodeListModel(Collections.EMPTY_LIST, namespaces);
            }
        }
        if (list.size() == 1) return new NodeListModel(list.get(0), namespaces);
        return new NodeListModel(list, namespaces);
    }

    /**
     * Returns true if this model contains no nodes.
     */
    public boolean isEmpty()
    {
        return nodes.isEmpty();
    }

    /**
     * This method returns the string resulting from concatenation
     * of string representations of its nodes. Each node is rendered using its XML
     * serialization format, while text (String) is rendered as itself. This greatly
     * simplifies creating XML-transformation templates, as to output a node contained
     * in variable x as XML fragment, you simply write ${x} in the template.
     */
    public String getAsString()
    throws
    TemplateModelException
    {
        if (isEmpty())
            return "";

        java.io.StringWriter sw = new java.io.StringWriter(nodes.size() * 128);
        try {
            for (Iterator i = nodes.iterator(); i.hasNext();) {
                Object node = i.next();
                if (node instanceof Element)
                    OUTPUT.output((Element)node, sw);
                else if (node instanceof Attribute)
                    OUTPUT.output((Attribute)node, sw);
                else if (node instanceof String)
                    sw.write(OUTPUT.escapeElementEntities(node.toString()));
                else if (node instanceof Text)
                    OUTPUT.output((Text)node, sw);
                else if (node instanceof Document)
                    OUTPUT.output((Document)node, sw);
                else if (node instanceof ProcessingInstruction)
                    OUTPUT.output((ProcessingInstruction)node, sw);
                else if (node instanceof Comment)
                    OUTPUT.output((Comment)node, sw);
                else if (node instanceof CDATA)
                    OUTPUT.output((CDATA)node, sw);
                else if (node instanceof DocType)
                    OUTPUT.output((DocType)node, sw);
                else if (node instanceof EntityRef)
                    OUTPUT.output((EntityRef)node, sw);
                else
                    throw new TemplateModelException(node.getClass().getName() + " is not a core JDOM class");
            }
        } catch (IOException e) {
            throw new TemplateModelException(e.getMessage());
        }
        return sw.toString();
    }


    /**
     * Provides node list traversal as well as special functions: filtering by name,
     * filtering by node type, shallow-copying, and duplicate removal.
     * While not as powerful as the full XPath support built into the
     * {@link #exec(List)} method, it does not require the external Jaxen
     * library to be present at run time. Below are listed the recognized keys.
     * In key descriptions, "applicable to this-and-that node type" means that if
     * a key is applied to a node list that contains a node of non-applicable type
     * a TemplateMethodModel will be thrown. However, you can use <tt>_ftype</tt>
     * key to explicitly filter out undesired node types prior to applying the
     * restricted-applicability key. Also "current nodes" means nodes contained in this
     * set.
     * <ul>
     *    <li><tt>*</tt> or <tt>_children</tt>: all direct element children of current nodes (non-recursive). Applicable
     *  to element and document nodes.</li>
     *    <li><tt>@*</tt> or <tt>_attributes</tt>: all attributes of current nodes. Applicable to elements only.</li>
     *    <li><tt>_content</tt> the complete content of current nodes (non-recursive).
     *  Applicable to elements and documents.</li>
     *    <li><tt>_text</tt>: the text of current nodes, one string per node (non-recursive).
     *  Applicable to elements, attributes, comments, processing instructions (returns its data)
     *  and CDATA sections. The reserved XML characters ('&lt;' and '&amp;') are escaped.</li>
     *    <li><tt>_plaintext</tt>: same as <tt>_text</tt>, but does not escape any characters,
     *  and instead of returning a NodeList returns a SimpleScalar.</li>
     *    <li><tt>_name</tt>: the names of current nodes, one string per node (non-recursive).
     *  Applicable to elements and attributes (returns their local name), 
     *  entities, processing instructions (returns its target), doctypes 
     * (returns its public ID)</li>
     *    <li><tt>_qname</tt>: the qualified names of current nodes in <tt>[namespacePrefix:]localName</tt>
     * form, one string per node (non-recursive). Applicable to elements and attributes</li>
     *    <li><tt>_cname</tt>: the canonical names of current nodes (namespace URI + local name),
     * one string per node (non-recursive). Applicable to elements and attributes</li>
     *    <li><tt>_nsprefix</tt>: namespace prefixes of current nodes,
     * one string per node (non-recursive). Applicable to elements and attributes</li>
     *    <li><tt>_nsuri</tt>: namespace URIs of current nodes,
     * one string per node (non-recursive). Applicable to elements and attributes</li>
     *    <li><tt>_parent</tt>: parent elements of current nodes. Applicable to element, attribute, comment,
     *  entity, processing instruction.</li>
     *    <li><tt>_ancestor</tt>: all ancestors up to root element (recursive) of current nodes. Applicable
     *  to same node types as <tt>_parent</tt>.</li>
     *    <li><tt>_ancestorOrSelf</tt>: all ancestors of current nodes plus current nodes. Applicable
     *  to same node types as <tt>_parent</tt>.</li>
     *    <li><tt>_descendant</tt>: all recursive descendant element children of current nodes. Applicable to
     *  document and element nodes.
     *    <li><tt>_descendantOrSelf</tt>: all recursive descendant element children of current nodes
     *  plus current nodes. Applicable to document and element nodes.
     *    <li><tt>_document</tt>: all documents the current nodes belong to.
     *  Applicable to all nodes except text.
     *    <li><tt>_doctype</tt>: doctypes of the current nodes.
     *  Applicable to document nodes only.
     *    <li><tt>_fname</tt>: is a filter-by-name template method model. When called,
     *  it will yield a node list that contains only those current nodes whose name
     *  matches one of names passed as argument. Attribute names should NOT be prefixed with the
     *  at sign (@). Applicable on all node types, however has no effect on unnamed nodes.</li>
     *    <li><tt>_ftype</tt>: is a filter-by-type template method model. When called,
     *  it will yield a node list that contains only those current nodes whose type matches one
     *  of types passed as argument. You should pass a single string to this method
     *  containing the characters of all types to keep. Valid characters are:
     *  e (Element), a (Attribute), n (Entity), d (Document), t (DocType),
     *  c (Comment), p (ProcessingInstruction), x (text). If the string anywhere contains
     *  the exclamation mark (!), the filter's effect is inverted.</li>
     *    <li><tt>_type</tt>: Returns a one-character String SimpleScalar containing
     *    the typecode of the first node in the node list. Valid characters are:
     *  e (Element), a (Attribute), n (Entity), d (Document), t (DocType),
     *  c (Comment), p (ProcessingInstruction), x (text). If the type of the node
     *  is unknown, returns '?'. If the node list is empty, returns an empty string scalar.</li>
     *    <li><tt>_unique</tt>: a copy of the current nodes that keeps only the
     *  first occurrence of every node, eliminating duplicates. Duplicates can
     *  occur in the node list by applying uptree-traversals <tt>_parent</tt>,
     *  <tt>_ancestor</tt>, <tt>_ancestorOrSelf</tt>, and <tt>_document</tt>.
     *  I.e. <tt>foo._children._parent</tt> will return a node list that has
     *  duplicates of nodes in foo - each node will have the number of occurrences
     *  equal to the number of its children. In these cases, use
     *  <tt>foo._children._parent._unique</tt> to eliminate duplicates. Applicable
     *  to all node types.</li>
     *    <li><tt>_copy</tt>: a copy of the current node list. It is a shallow copy that
     *  shares the underlying node list with this node list, however it has a
     *  separate namespace registry, so it can be used to guarantee that subsequent
     *  changes to the set of registered namespaces does not affect the node lists
     *  that were used to create this node list. Applicable to all node types.</li>
     *    <li><tt>_registerNamespace(prefix, uri)</tt>: register a XML namespace
     *  with the specified prefix and URI for the current node list and all node
     *  lists that are derived from the current node list. After registering,
     *  you can use the <tt>nodelist["prefix:localname"]</tt> or
     *  <tt>nodelist["@prefix:localname"]</tt> syntaxes to reach elements and
     *  attributes whose names are namespace-scoped. Note that the namespace
     *  prefix need not match the actual prefix used by the XML document itself
     *  since namespaces are compared solely by their URI. You can also register
     *  namespaces from Java code using the
     *  {@link #registerNamespace(String, String)} method.
     * </li>
     *    <li><tt>@attributeName</tt>: named attributes of current nodes. Applicable to
     *  elements, doctypes and processing instructions. On doctypes it supports
     *  attributes <tt>publicId</tt>, <tt>systemId</tt> and <tt>elementName</tt>. On processing
     *  instructions, it supports attributes <tt>target</tt> and <tt>data</tt>, as
     *  well as any other attribute name specified in data as <tt>name="value"</tt> pair.
     *  The attribute nodes for doctype and processing instruction are synthetic, and
     *  as such have no parent. Note, however that <tt>@*</tt> does NOT operate on
     *  doctypes or processing instructions.</li>
     *    <li>any other key: element children of current nodes with name matching the key.
     *  This allows for convenience child traversal in <tt>book.chapter.title</tt> style syntax.
     *  Note that <tt>nodeset.childname</tt> is technically equivalent to
     *  <tt>nodeset._children._fname("childname")</tt>, but is both shorter to write
     *  and evaluates faster. Applicable to document and element nodes.</li>
     * </ul>
     * The order of nodes in the resulting set is the order of evaluation of the key
     * on each node in this set from left to right. Evaluation of the key on a single
     * node always yields the results in "natural" order (that of the document preorder
     * traversal), even for uptree traversals. As a consequence, if this node list's nodes
     * are listed in natural order, applying any of the keys will produce a node list that
     * is also naturally ordered. As a special case, all node lists that are directly or
     * indirectly generated from a single Document or Element node through repeated
     * invocations of this method will be naturally ordered.
     * @param key a key that identifies a required set of nodes
     * @return a new NodeListModel that represents the requested set of nodes.
     */
    public TemplateModel get(String key)
    throws
    TemplateModelException
    {
        if (isEmpty())
            return EMPTY;

        if (key == null || key.length() == 0)
            throw new TemplateModelException("Invalid key [" + key + "]");

        NodeOperator op = null;
        NamedNodeOperator nop = null;
        String name = null;

        switch (key.charAt(0)) {
            case '@':
                {
                    if (key.length() != 2 || key.charAt(1) != '*') {
                        // Generic attribute key
                        nop = NAMED_ATTRIBUTE_OP;
                        name = key.substring(1);
                    } else
                        // It is @*
                        op = ALL_ATTRIBUTES_OP;

                    break;
                }
            case '*':
                {
                    if (key.length() == 1)
                        op = ALL_CHILDREN_OP;
                    else
                        // Explicitly disallow any other identifier starting with asterisk
                        throw new TemplateModelException("Invalid key [" + key + "]");

                    break;
                }
            case 'x':
            case '_':
                {
                    op = (NodeOperator)OPERATIONS.get(key);
                    if (op == null) {
                        // Some special operation?
                        Integer specop = (Integer)SPECIAL_OPERATIONS.get(key);
                        if (specop != null) {
                            switch (specop.intValue()) {
                                case SPECIAL_OPERATION_COPY:
                                {
                                    synchronized(namespaces)
                                    {
                                        return new NodeListModel(nodes, (Map)((HashMap)namespaces).clone());
                                    }
                                }
                                case SPECIAL_OPERATION_UNIQUE:
                                    return new NodeListModel(removeDuplicates(nodes), namespaces);
                                case SPECIAL_OPERATION_FILTER_NAME:
                                    return new NameFilter();
                                case SPECIAL_OPERATION_FILTER_TYPE:
                                    return new TypeFilter();
                                case SPECIAL_OPERATION_QUERY_TYPE:
                                    return getType();
                                case SPECIAL_OPERATION_REGISTER_NAMESPACE:
                                    return new RegisterNamespace();
                                case SPECIAL_OPERATION_PLAINTEXT:
                                    return getPlainText();
                            }
                        }
                    }
                    break;
                }
        }

        if (op == null && nop == null) {
            nop = NAMED_CHILDREN_OP;
            name = key;
        }

        List list = null;
        if (op != null)
            list = evaluateElementOperation(op, nodes);
        else {
            String localName = name;
            Namespace namespace = Namespace.NO_NAMESPACE;
            int colon = name.indexOf(':');
            if (colon != -1) {
                localName = name.substring(colon + 1);
                String nsPrefix = name.substring(0, colon);
                synchronized(namespaces)
                {
                    namespace = (Namespace)namespaces.get(nsPrefix);
                }
                if (namespace == null) {
                    if (nsPrefix.equals("xml"))
                        namespace = Namespace.XML_NAMESPACE;
                    else
                        throw new TemplateModelException("Unregistered namespace prefix '" + nsPrefix + "'");
                }
            }

            list = evaluateNamedElementOperation(nop, localName, namespace, nodes);
        }
        return createNodeListModel(list, namespaces);
    }

    private TemplateModel getType()
    {
        if (nodes.size() == 0)
            return new SimpleScalar("");
        Object firstNode = nodes.get(0);
        char code;
        if (firstNode instanceof Element)
            code = 'e';
        else if (firstNode instanceof Text || firstNode instanceof String)
            code = 'x';
        else if (firstNode instanceof Attribute)
            code = 'a';
        else if (firstNode instanceof EntityRef)
            code = 'n';
        else if (firstNode instanceof Document)
            code = 'd';
        else if (firstNode instanceof DocType)
            code = 't';
        else if (firstNode instanceof Comment)
            code = 'c';
        else if (firstNode instanceof ProcessingInstruction)
            code = 'p';
        else
            code = '?';
        return new SimpleScalar(new String(new char[] { code}));
    }

    private SimpleScalar getPlainText()
    throws
    TemplateModelException
    {
        List list = evaluateElementOperation((TextOp)OPERATIONS.get("_text"), nodes);
        StringBuffer buf = new StringBuffer();
        for (Iterator it = list.iterator(); it.hasNext();) {
            buf.append(it.next());
        }
        return new SimpleScalar(buf.toString());
    }

    public TemplateModelIterator iterator()
    {
        return new TemplateModelIterator()
        {
            private final Iterator it = nodes.iterator();

            public TemplateModel next()
            {
                return it.hasNext() ? new NodeListModel(it.next(), namespaces) : null;
            }

            public boolean hasNext() 
            {
                return it.hasNext();
            }
        };
    }

    /**
     * Retrieves the i-th element of the node list.
     */
    public TemplateModel get(int i)
    throws
    TemplateModelException
    {
        try {
            return new NodeListModel(nodes.get(i), namespaces);
        } catch (IndexOutOfBoundsException e) {
            throw new TemplateModelException("Index out of bounds: " + e.getMessage());
        }
    }
    
    public int size()
    {
        return nodes.size();
    }

    /**
     * Applies an XPath expression to the node list and returns the resulting node list.
     * In order for this method to work, your application must have access
     * <a href="http://www.jaxen.org">Jaxen</a> library classes. The
     * implementation does cache the parsed format of XPath expressions in a weak hash
     * map, keyed by the string representation of the XPath expression. As the string
     * object passed as the argument is usually kept in the parsed FreeMarker template,
     * this ensures that each XPath expression is parsed only once during the lifetime
     * of the FreeMarker template that contains it.
     * @param arguments the list of arguments. Must contain exactly one string that is
     * the XPath expression you wish to apply. The XPath expression can use any namespace
     * prefixes that were defined using the {@link #registerNamespace(String, String)}
     * method or the <code>nodelist._registerNamespace(prefix, uri)</code> expression in the
     * template.
     * @return a NodeListModel representing the nodes that are the result of application
     * of the XPath to the current node list.
     */
    public Object exec(List arguments)
    throws
    TemplateModelException
    {
        if (arguments == null || arguments.size() != 1)
            throw new TemplateModelException("Exactly one argument required for execute() on NodeTemplate");

        String xpathString = (String)arguments.get(0);
        JDOMXPathEx xpath = null;
        try
        {
            synchronized(XPATH_CACHE)
            {
                xpath = (JDOMXPathEx)XPATH_CACHE.get(xpathString);
                if (xpath == null)
                {
                    xpath = new JDOMXPathEx(xpathString);
                    XPATH_CACHE.put(xpathString, xpath);
                }
            }
            return createNodeListModel(xpath.selectNodes(nodes, namespaces), namespaces);
        }
        catch(Exception e)
        {
            throw new TemplateModelException("Could not evaulate XPath expression " + xpathString, e);
        }
    }

    /**
     * Registers an XML namespace with this node list. Once registered, you can
     * refer to the registered namespace using its prefix in the
     * {@link #get(String)} method from this node list and all other
     * node lists that are derived from this node list. Use the
     * <tt>nodelist["prefix:localname"]</tt> or the
     * <tt>nodelist["@prefix:localname"]</tt> syntax to reach elements and
     *  attributes whose names are namespace-scoped. Note that the namespace
     * prefix need not match the actual prefix used by the XML document itself
     * since namespaces are compared solely by their URI. You can also register
     * namespaces during template evaluation using the
     * <tt>nodelist._registerNamespace(prefix, uri)</tt> syntax in the template.
     * This mechanism is completely independent from the namespace declarations
     * in the XML document itself; its purpose is to give you an easy way
     * to refer to namespace-scoped elements in {@link #get(String)} and
     * in XPath expressions passed to {@link #exec(List)}. Note also that
     * the namespace prefix registry is shared among all node lists that
     * are created from a single node list - modifying the registry in one
     * affects all others as well. If you want to obtain a namespace 
     * "detached" copy of the node list, use the <code>_copy</code> key on
     * it (or call <code>nodeList.get("_copy")</code> directly from your
     * Java code. The returned node list has all the namespaces that the
     * original node list has, but they can be manipulated independently
     * thereon.
     */
    public void registerNamespace(String prefix, String uri)
    {
        synchronized(namespaces)
        {
            namespaces.put(prefix, Namespace.getNamespace(prefix, uri));
        }
    }

    private interface NodeOperator {
        List operate(Object node)
        throws
        TemplateModelException;
    }

    private interface NamedNodeOperator {
        List operate(Object node, String localName, Namespace namespace)
        throws
        TemplateModelException;
    }

    private static final class AllChildrenOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
                return((Element)node).getChildren();
            else if (node instanceof Document) {
                Element root = ((Document)node).getRootElement();
                return root == null ? Collections.EMPTY_LIST : Collections.singletonList(root);
            } 
 // With 2.1 semantics it  makes more sense to just return a null and let the core 
 // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
/*            
            else
                throw new TemplateModelException("_allChildren can not be applied on " + node.getClass());
*/                
        }
    }

    private static final class NamedChildrenOp implements NamedNodeOperator {
        public List operate(Object node, String localName, Namespace namespace)
        {
            if (node instanceof Element) {
                return((Element)node).getChildren(localName, namespace);
            } else if (node instanceof Document) {
                Element root = ((Document)node).getRootElement();
                if (root != null &&
                    root.getName().equals(localName) &&
                    root.getNamespaceURI().equals(namespace.getURI())) {
                    return Collections.singletonList(root);
                } else
                    return Collections.EMPTY_LIST;
            } 
 // With 2.1 semantics it  makes more sense to just return a null and let the core 
 // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
 /*           
            else
                throw new TemplateModelException("_namedChildren can not be applied on " + node.getClass());
*/                
        }
    }

    private static final class AllAttributesOp implements NodeOperator {
        public List operate(Object node)
        {
 // With 2.1 semantics it  makes more sense to just return a null and let the core 
 // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            if (!(node instanceof Element)) {
                return null;
            }
            return ((Element)node).getAttributes();
/*
            else
                throw new TemplateModelException("_allAttributes can not be applied on " + node.getClass());
*/                
        }
    }

    private static final class NamedAttributeOp implements NamedNodeOperator {
        public List operate(Object node, String localName, Namespace namespace)
        {
            Attribute attr = null;
            if (node instanceof Element) {
                Element element = (Element)node;
                attr = element.getAttribute(localName, namespace);
            } else if (node instanceof ProcessingInstruction) {
                ProcessingInstruction pi = (ProcessingInstruction)node;
                if ("target".equals(localName))
                    attr = new Attribute("target", pi.getTarget());
                else if ("data".equals(localName))
                    attr = new Attribute("data", pi.getData());
                else
                    attr = new Attribute(localName, pi.getValue(localName));
            } else if (node instanceof DocType) {
                DocType doctype = (DocType)node;
                if ("publicId".equals(localName))
                    attr = new Attribute("publicId", doctype.getPublicID());
                else if ("systemId".equals(localName))
                    attr = new Attribute("systemId", doctype.getSystemID());
                else if ("elementName".equals(localName))
                    attr = new Attribute("elementName", doctype.getElementName());
            } 
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            else {
                return null;
            }
/*            
            else
                throw new TemplateModelException("_allAttributes can not be applied on " + node.getClass());
*/
            return attr == null ? Collections.EMPTY_LIST : Collections.singletonList(attr);
        }
    }

    private static final class NameOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
                return Collections.singletonList(((Element)node).getName());
            else if (node instanceof Attribute)
                return Collections.singletonList(((Attribute)node).getName());
            else if (node instanceof EntityRef)
                return Collections.singletonList(((EntityRef)node).getName());
            else if (node instanceof ProcessingInstruction)
                return Collections.singletonList(((ProcessingInstruction)node).getTarget());
            else if (node instanceof DocType)
                return Collections.singletonList(((DocType)node).getPublicID());
            else
                return null;
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
//                throw new TemplateModelException("_name can not be applied on " + node.getClass());
        }
    }

    private static final class QNameOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
                return Collections.singletonList(((Element)node).getQualifiedName());
            else if (node instanceof Attribute)
                return Collections.singletonList(((Attribute)node).getQualifiedName());
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
//            throw new TemplateModelException("_qname can not be applied on " + node.getClass());
        }
    }

    private static final class NamespaceUriOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
                return Collections.singletonList(((Element)node).getNamespace().getURI());
            else if (node instanceof Attribute)
                return Collections.singletonList(((Attribute)node).getNamespace().getURI());
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
//            throw new TemplateModelException("_nsuri can not be applied on " + node.getClass());
        }
    }

    private static final class NamespacePrefixOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
                return Collections.singletonList(((Element)node).getNamespace().getPrefix());
            else if (node instanceof Attribute)
                return Collections.singletonList(((Attribute)node).getNamespace().getPrefix());
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
//            throw new TemplateModelException("_nsprefix can not be applied on " + node.getClass());
        }
    }

    private static final class CanonicalNameOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
            {
                Element element = (Element)node;
                return Collections.singletonList(element.getNamespace().getURI() + element.getName());
            }
            else if (node instanceof Attribute)
            {
                Attribute attribute = (Attribute)node;
                return Collections.singletonList(attribute.getNamespace().getURI() + attribute.getName());
            }
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
//            throw new TemplateModelException("_cname can not be applied on " + node.getClass());
        }
    }


    private static final Element getParent(Object node)
    {
        if (node instanceof Element)
            return((Element)node).getParent();
        else if (node instanceof Attribute)
            return((Attribute)node).getParent();
        else if (node instanceof Text)
            return((Text)node).getParent();
        else if (node instanceof ProcessingInstruction)
            return((ProcessingInstruction)node).getParent();
        else if (node instanceof Comment)
            return((Comment)node).getParent();
        else if (node instanceof EntityRef)
            return((EntityRef)node).getParent();
        else
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
//            throw new TemplateModelException("_parent can not be applied on " + node.getClass());
    }

    private static final class ParentOp implements NodeOperator {
        public List operate(Object node)
        {
            Element parent = getParent(node);
            return parent == null ? Collections.EMPTY_LIST : Collections.singletonList(parent);
        }
    }

    private static final class AncestorOp implements NodeOperator {
        public List operate(Object node)
        {
            Element parent = getParent(node);
            if (parent == null) return Collections.EMPTY_LIST;
            LinkedList list = new LinkedList();
            do {
                list.addFirst(parent);
                parent = parent.getParent();
            }
            while (parent != null);
            return list;
        }
    }

    private static final class AncestorOrSelfOp implements NodeOperator {
        public List operate(Object node)
        {
            Element parent = getParent(node);
            if (parent == null) return Collections.singletonList(node);
            LinkedList list = new LinkedList();
            list.addFirst(node);
            do {
                list.addFirst(parent);
                parent = parent.getParent();
            }
            while (parent != null);
            return list;
        }
    }

    private static class DescendantOp implements NodeOperator {
        public List operate(Object node)
        {
            LinkedList list = new LinkedList();
            if (node instanceof Element) {
                addChildren((Element)node, list);
            }
            else if (node instanceof Document) {
                Element root = ((Document)node).getRootElement();
                list.add(root);
                addChildren(root, list);
            }
            else
                // With 2.1 semantics it  makes more sense to just return a null and let the core 
                // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
                return null;
//                throw new TemplateModelException("_descendant can not be applied on " + node.getClass());

            return list;
        }

        private void addChildren(Element element, List list)
        {
            List children = element.getChildren();
            Iterator it = children.iterator();
            while (it.hasNext()) {
                Element child = (Element)it.next();
                list.add(child);
                addChildren(child, list);
            }
        }
    }

    private static final class DescendantOrSelfOp extends DescendantOp {
        public List operate(Object node)
        {
            LinkedList list = (LinkedList)super.operate(node);
            list.addFirst(node);
            return list;
        }
    }

    private static final class DocumentOp implements NodeOperator {
        public List operate(Object node)
        {
            Document doc = null;
            if (node instanceof Element)
                doc = ((Element)node).getDocument();
            else if (node instanceof Attribute) {
                Element parent = ((Attribute)node).getParent();
                doc = parent == null ? null : parent.getDocument();
            } else if (node instanceof Text) {
                Element parent = ((Text)node).getParent();
                doc = parent == null ? null : parent.getDocument();
            } else if (node instanceof Document)
                doc = (Document)node;
            else if (node instanceof ProcessingInstruction)
                doc = ((ProcessingInstruction)node).getDocument();
            else if (node instanceof EntityRef)
                doc = ((EntityRef)node).getDocument();
            else if (node instanceof Comment)
                doc = ((Comment)node).getDocument();
            else
                // With 2.1 semantics it  makes more sense to just return a null and let the core 
                // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
                return null;
//                throw new TemplateModelException("_document can not be applied on " + node.getClass());

            return doc == null ? Collections.EMPTY_LIST : Collections.singletonList(doc);
        }
    }

    private static final class DocTypeOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Document) {
                DocType doctype = ((Document)node).getDocType();
                return doctype == null ? Collections.EMPTY_LIST : Collections.singletonList(doctype);
            } else
                // With 2.1 semantics it  makes more sense to just return a null and let the core 
                // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
                return null;
//                throw new TemplateModelException("_doctype can not be applied on " + node.getClass());
        }
    }

    private static final class ContentOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
                return((Element)node).getContent();
            else if (node instanceof Document)
                return((Document)node).getContent();
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
//            throw new TemplateModelException("_content can not be applied on " + node.getClass());
        }
    }

    private static final class TextOp implements NodeOperator {
        public List operate(Object node)
        {
            if (node instanceof Element)
                return Collections.singletonList(((Element)node).getTextTrim());
            if (node instanceof Attribute)
                return Collections.singletonList(((Attribute)node).getValue());
            if (node instanceof CDATA)
                return Collections.singletonList(((CDATA)node).getText());
            if (node instanceof Comment)
                return Collections.singletonList(((Comment)node).getText());
            if (node instanceof ProcessingInstruction)
                return Collections.singletonList(((ProcessingInstruction)node).getData());
            // With 2.1 semantics it  makes more sense to just return a null and let the core 
            // throw an InvalidReferenceException and the template writer can use ?exists etcetera. (JR)
            return null;
//            throw new TemplateModelException("_text can not be applied on " + node.getClass());
        }
    }

    private static final List evaluateElementOperation(NodeOperator op, List nodes)
    throws
    TemplateModelException
    {
        int s = nodes.size();
        List[] lists = new List[s];
        int l = 0;
        {
            int i = 0;
            Iterator it = nodes.iterator();
            while (it.hasNext()) {
                List list = op.operate(it.next());
                if (list != null) {
                    lists[i++] = list;
                    l += list.size();
                }
            }
        }
        List retval = new ArrayList(l);
        for (int i = 0; i < s; ++i) {
            if (lists[i] != null) {
                retval.addAll(lists[i]);
            }
        }
        return retval;
    }

    private static final List evaluateNamedElementOperation(NamedNodeOperator op, String localName, Namespace namespace, List nodes)
    throws
    TemplateModelException
    {
        int s = nodes.size();
        List[] lists = new List[s];
        int l = 0;
        {
            int i = 0;
            Iterator it = nodes.iterator();
            while (it.hasNext()) {
                List list = op.operate(it.next(), localName, namespace);
                lists[i++] = list;
                l += list.size();
            }
        }
        List retval = new ArrayList(l);
        for (int i = 0; i < s; ++i)
            retval.addAll(lists[i]);
        return retval;
    }

    private static final List removeDuplicates(List list)
    {
        int s = list.size();
        ArrayList ulist = new ArrayList(s);
        Set set = new HashSet(s * 4 / 3, .75f);
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Object o = it.next();
            if (set.add(o))
                ulist.add(o);
        }
        ulist.trimToSize();
        return ulist;
    }

    private static final Map createOperations()
    {
        Map map = new HashMap();

        map.put("_ancestor", new AncestorOp());
        map.put("_ancestorOrSelf", new AncestorOrSelfOp());
        map.put("_attributes", ALL_ATTRIBUTES_OP);
        map.put("_children", ALL_CHILDREN_OP);
        map.put("_cname", new CanonicalNameOp());
        map.put("_content", new ContentOp());
        map.put("_descendant", new DescendantOp());
        map.put("_descendantOrSelf", new DescendantOrSelfOp());
        map.put("_document", new DocumentOp());
        map.put("_doctype", new DocTypeOp());
        map.put("_name", new NameOp());
        map.put("_nsprefix", new NamespacePrefixOp());
        map.put("_nsuri", new NamespaceUriOp());
        map.put("_parent", new ParentOp());
        map.put("_qname", new QNameOp());
        map.put("_text", new TextOp());

        return map;
    }

    private static final Map createSpecialOperations()
    {
        Map map = new HashMap();

        Integer copy = new Integer(SPECIAL_OPERATION_COPY);
        Integer unique = new Integer(SPECIAL_OPERATION_UNIQUE);
        Integer fname = new Integer(SPECIAL_OPERATION_FILTER_NAME);
        Integer ftype = new Integer(SPECIAL_OPERATION_FILTER_TYPE);
        Integer type = new Integer(SPECIAL_OPERATION_QUERY_TYPE);
        Integer regns = new Integer(SPECIAL_OPERATION_REGISTER_NAMESPACE);
        Integer plaintext = new Integer(SPECIAL_OPERATION_PLAINTEXT);

        map.put("_copy", copy);
        map.put("_unique", unique);
        map.put("_fname", fname);
        map.put("_ftype", ftype);
        map.put("_type", type);
        map.put("_registerNamespace", regns);
        map.put("_plaintext", plaintext);

        // These are in for backward compatibility
        map.put("x_copy", copy);
        map.put("x_unique", unique);
        map.put("x_fname", fname);
        map.put("x_ftype", ftype);
        map.put("x_type", type);

        return map;
    }

    private final class RegisterNamespace implements TemplateMethodModel {
        public boolean isEmpty()
        {
            return false;
        }

        public Object exec(List arguments)
        throws
        TemplateModelException
        {
            if (arguments.size() != 2)
                throw new TemplateModelException("_registerNamespace(prefix, uri) requires two arguments");

            registerNamespace((String)arguments.get(0), (String)arguments.get(1));

            return TemplateScalarModel.EMPTY_STRING;
        }
    }

    private final class NameFilter implements TemplateMethodModel {
        public boolean isEmpty()
        {
            return false;
        }

        public Object exec(List arguments)
        {
            Set names = new HashSet(arguments);
            List list = new LinkedList(nodes);
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object node = it.next();
                String name = null;
                if (node instanceof Element)
                    name = ((Element)node).getName();
                else if (node instanceof Attribute)
                    name = ((Attribute)node).getName();
                else if (node instanceof ProcessingInstruction)
                    name = ((ProcessingInstruction)node).getTarget();
                else if (node instanceof EntityRef)
                    name = ((EntityRef)node).getName();
                else if (node instanceof DocType)
                    name = ((DocType)node).getPublicID();

                if (name == null || !names.contains(name))
                    it.remove();
            }
            return createNodeListModel(list, namespaces);
        }
    }

    private final class TypeFilter implements TemplateMethodModel {
        public boolean isEmpty()
        {
            return false;
        }

        public Object exec(List arguments)
        throws
        TemplateModelException
        {
            if (arguments == null || arguments.size() == 0)
                throw new TemplateModelException("_type expects exactly one argument");
            String arg = (String)arguments.get(0);
            boolean invert = arg.indexOf('!') != -1;
            // NOTE: true in each of these variables means 'remove', not 'keep'
            // This is so we don't invert their values in the loop. So,
            // a is true <--> (a is not present in the string) xor invert.
            boolean a = invert != (arg.indexOf('a') == -1);
            boolean c = invert != (arg.indexOf('c') == -1);
            boolean d = invert != (arg.indexOf('d') == -1);
            boolean e = invert != (arg.indexOf('e') == -1);
            boolean n = invert != (arg.indexOf('n') == -1);
            boolean p = invert != (arg.indexOf('p') == -1);
            boolean t = invert != (arg.indexOf('t') == -1);
            boolean x = invert != (arg.indexOf('x') == -1);

            LinkedList list = new LinkedList(nodes);
            Iterator it = list.iterator();
            while (it.hasNext()) {
                Object node = it.next();
                if ((node instanceof Element && e)
                    || (node instanceof Attribute && a)
                    || (node instanceof String && x)
                    || (node instanceof Text && x)
                    || (node instanceof ProcessingInstruction && p)
                    || (node instanceof Comment && c)
                    || (node instanceof EntityRef && n)
                    || (node instanceof Document && d)
                    || (node instanceof DocType && t))
                    it.remove();
            }
            return createNodeListModel(list, namespaces);
        }
    }

    /**
     * Loads a template from a file passed as the first argument, loads an XML
     * document from the standard input, passes it to the template as variable
     * <tt>document</tt> and writes the result of template processing to
     * standard output.
     * 
     * @deprecated Will be removed (main method in a library, often classified as CWE-489 "Leftover Debug Code").
     */
    public static void main(String[] args)
    throws
    Exception
    {
        org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
        Document document = builder.build(System.in);
        SimpleHash model = new SimpleHash();
        model.put("document", new NodeListModel(document));
        FileReader fr = new FileReader(args[0]);
        Template template = new Template(args[0], fr);
        Writer w = new java.io.OutputStreamWriter(System.out);
        template.process(model, w);
        w.flush();
        w.close();
    }

    private static final class AttributeXMLOutputter extends XMLOutputter {
        public void output(Attribute attribute, Writer out)
        throws
        IOException
        {
            out.write(" ");
            out.write(attribute.getQualifiedName());
            out.write("=");

            out.write("\"");
            out.write(escapeAttributeEntities(attribute.getValue()));
            out.write("\"");
        }
    }

    private static final class JDOMXPathEx
    extends
        JDOMXPath
    {
        JDOMXPathEx(String path)
        throws 
            JaxenException
        {
            super(path);
        }

        public List selectNodes(Object object, Map namespaces)
        throws
            JaxenException
        {
            Context context = getContext(object);
            context.getContextSupport().setNamespaceContext(new NamespaceContextImpl(namespaces));
            return selectNodesForContext(context);
        } 

        private static final class NamespaceContextImpl
        implements
            NamespaceContext
        {
            private final Map namespaces;
            
            NamespaceContextImpl(Map namespaces)
            {
                this.namespaces = namespaces;
            }
            
            public String translateNamespacePrefixToUri(String prefix)
            {
                // Empty prefix always maps to empty URL in XPath
                if(prefix.length() == 0)
                {
                    return prefix;
                }
                synchronized(namespaces)
                {
                    Namespace ns = (Namespace)namespaces.get(prefix);
                    return ns == null ? null : ns.getURI();
                }   
            }
        }
    }
}
