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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import freemarker.ext.dom.NodeModel;

/**
 * <p>The default implementation of the ObjectWrapper
 * interface.
 */
public class DefaultObjectWrapper extends freemarker.ext.beans.BeansWrapper {
    
    static final DefaultObjectWrapper instance = new DefaultObjectWrapper();
    
    static private Class W3C_DOM_NODE_CLASS, 
                         JYTHON_OBJ_CLASS;
    
    static private ObjectWrapper JYTHON_WRAPPER;
    
    static {
        try {
            W3C_DOM_NODE_CLASS = Class.forName("org.w3c.dom.Node");
        } catch (Exception e) {}
        try {
            JYTHON_OBJ_CLASS = Class.forName("org.python.core.PyObject");
            JYTHON_WRAPPER = (ObjectWrapper) Class.forName(
                    "freemarker.ext.jython.JythonWrapper")
                    .getField("INSTANCE").get(null);
        } catch (Exception e) {
            // ignore
        }
    }

    public TemplateModel wrap(Object obj) throws TemplateModelException {
        if (obj == null) {
            return super.wrap(null);
        }
        if (obj instanceof TemplateModel) {
            return (TemplateModel) obj;
        }
        if (obj instanceof String) {
            return new SimpleScalar((String) obj);
        }
        if (obj instanceof Number) {
            return new SimpleNumber((Number) obj);
        }
        if (obj instanceof java.util.Date) {
            if(obj instanceof java.sql.Date) {
                return new SimpleDate((java.sql.Date) obj);
            }
            if(obj instanceof java.sql.Time) {
                return new SimpleDate((java.sql.Time) obj);
            }
            if(obj instanceof java.sql.Timestamp) {
                return new SimpleDate((java.sql.Timestamp) obj);
            }
            return new SimpleDate((java.util.Date) obj, getDefaultDateType());
        }
        if (obj.getClass().isArray()) {
            obj = convertArray(obj);
        }
        if (obj instanceof Collection) {
            return new SimpleSequence((Collection) obj, this);
        }
        if (obj instanceof Map) {
            return new SimpleHash((Map) obj, this);
        }
        if (obj instanceof Boolean) {
            return obj.equals(Boolean.TRUE) ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        if (obj instanceof Iterator) {
            return new SimpleCollection((Iterator) obj, this);
        }
        return handleUnknownType(obj);
    }
    
    
    /**
     * Called if an unknown type is passed in.
     * Since 2.3, this falls back on XML wrapper and BeansWrapper functionality.
     */
    protected TemplateModel handleUnknownType(Object obj) throws TemplateModelException {
        if ((W3C_DOM_NODE_CLASS != null && W3C_DOM_NODE_CLASS.isInstance(obj)))
        {
            return wrapDomNode(obj);
        }
        if (JYTHON_WRAPPER != null  && JYTHON_OBJ_CLASS.isInstance(obj)) {
            return JYTHON_WRAPPER.wrap(obj);
        }
        return super.wrap(obj); 
    }

    
    public TemplateModel wrapDomNode(Object obj) {
        return NodeModel.wrap((org.w3c.dom.Node) obj);
    }

    /**
     * Converts an array to a java.util.List
     */
    protected Object convertArray(Object arr) {
        final int size = Array.getLength(arr);
        ArrayList list = new ArrayList(size);
        for (int i=0;i<size; i++) {
            list.add(Array.get(arr, i));
        }
        return list;
    }
}
