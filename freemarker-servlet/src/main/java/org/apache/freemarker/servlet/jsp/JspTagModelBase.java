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

package org.apache.freemarker.servlet.jsp;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import javax.servlet.jsp.tagext.JspTag;

import org.apache.freemarker.core.Template;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._DelayedJQuote;
import org.apache.freemarker.core._DelayedShortClassName;
import org.apache.freemarker.core._ErrorDescriptionBuilder;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModelEx2;
import org.apache.freemarker.core.model.TemplateModelWithOriginName;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.model.impl.DefaultObjectWrapper;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.servlet.jsp.SimpleTagDirectiveModel.TemplateExceptionWrapperJspException;

abstract class JspTagModelBase implements TemplateModelWithOriginName {
    protected final String tagName;
    private final Class tagClass;
    private final Map propertySetters = new HashMap();
    
    protected JspTagModelBase(String tagName, Class tagClass) throws IntrospectionException {
        this.tagName = tagName;
        this.tagClass = tagClass;
        BeanInfo bi = Introspector.getBeanInfo(tagClass);
        PropertyDescriptor[] pda = bi.getPropertyDescriptors();
        for (PropertyDescriptor pd : pda) {
            Method m = pd.getWriteMethod();
            if (m != null) {
                propertySetters.put(pd.getName(), m);
            }
        }
    }
    
    Object getTagInstance() throws IllegalAccessException, InstantiationException {
        return tagClass.newInstance();
    }
    
    void setupTag(JspTag tag, TemplateHashModelEx2 args, ObjectWrapperAndUnwrapper wrapper)
            throws TemplateException,
        InvocationTargetException, 
        IllegalAccessException {
        if (args != null && !args.isEmpty()) {
            final Object[] argArray = new Object[1];
            for (TemplateHashModelEx2.KeyValuePairIterator iter = args.keyValuePairIterator(); iter.hasNext(); ) {
                final TemplateHashModelEx2.KeyValuePair entry = iter.next();
                final Object arg = wrapper.unwrap(entry.getValue());
                argArray[0] = arg;
                final String paramName = ((TemplateStringModel) entry.getKey()).getAsString();
                Method setterMethod = (Method) propertySetters.get(paramName);
                if (setterMethod == null) {
                    if (tag instanceof DynamicAttributes) {
                        try {
                            ((DynamicAttributes) tag).setDynamicAttribute(null, paramName, argArray[0]);
                        } catch (JspException e) {
                            throw new TemplateException(
                                    "Failed to set JSP tag dynamic attribute ", new _DelayedJQuote(paramName), ".",
                                    e);
                        }
                    } else {
                        throw new TemplateException("Unknown property "
                                + _StringUtils.jQuote(paramName.toString())
                                + " on instance of " + tagClass.getName());
                    }
                } else {
                    if (arg instanceof BigDecimal) {
                        argArray[0] = DefaultObjectWrapper.coerceBigDecimal(
                                (BigDecimal) arg, setterMethod.getParameterTypes()[0]);
                    }
                    try {
                        setterMethod.invoke(tag, argArray);
                    } catch (Exception e) {
                        final Class setterType = setterMethod.getParameterTypes()[0];
                        final _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(
                                "Failed to set JSP tag parameter ", new _DelayedJQuote(paramName),
                                " (declared type: ", new _DelayedShortClassName(setterType)
                                + ", actual value's type: ",
                                (argArray[0] != null
                                        ? new _DelayedShortClassName(argArray[0].getClass()) : "Null"),
                                "). See cause exception for the more specific cause...");
                        if (e instanceof IllegalArgumentException && !(setterType.isAssignableFrom(String.class))
                                && argArray[0] != null && argArray[0] instanceof String) {
                            desc.tip("This problem is often caused by unnecessary parameter quotation. Paramters "
                                    + "aren't quoted in FTL, similarly as they aren't quoted in most languages. "
                                    + "For example, these parameter assignments are wrong: ",
                                    "<@my.tag p1=\"true\" p2=\"10\" p3=\"${someVariable}\" p4=\"${x+1}\" />",
                                    ". The correct form is: ",
                                    "<@my.tag p1=true p2=10 p3=someVariable p4=x+1 />",
                                    ". Only string literals are quoted (regardless of where they occur): ",
                                    "<@my.box style=\"info\" message=\"Hello ${name}!\" width=200 />",
                                    ".");
                        }
                        throw new TemplateException(e, null, desc);
                    }
                }
            }
        }
    }

    protected final TemplateException toTemplateExceptionOrRethrow(Throwable e)
            throws TemplateException {
        if (e instanceof RuntimeException && !isCommonRuntimeException((RuntimeException) e)) {
            throw (RuntimeException) e;
        }
        if (e instanceof TemplateException) {
            throw (TemplateException) e;
        }
        if (e instanceof TemplateExceptionWrapperJspException) {
            return (TemplateException) e.getCause();
        }
        return new TemplateException(e,
                "Error while invoking the ", new _DelayedJQuote(tagName), " JSP custom tag; see cause exception");
    }

    /**
     * Runtime exceptions that we don't want to propagate, instead we warp them into a more helpful exception. These are
     * the ones where it's very unlikely that someone tries to catch specifically these around
     * {@link Template#process(Object, java.io.Writer)}.
     */
    private boolean isCommonRuntimeException(RuntimeException e) {
        final Class eClass = e.getClass();
        // We deliberately don't accept sub-classes. Those are possibly application specific and some want to catch them
        // outside the template.
        return eClass == NullPointerException.class
                || eClass == IllegalArgumentException.class
                || eClass == ClassCastException.class
                || eClass == IndexOutOfBoundsException.class;
    }

    @Override
    public String getOriginName() {
        // TODO Can't we know the namespace URI from somewhere?
        return "jspCustomTag:" + tagName;
    }
}
