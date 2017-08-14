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

package org.apache.freemarker.core.model.impl;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._CallableUtils;
import org.apache.freemarker.core._DelayedJQuote;
import org.apache.freemarker.core._TemplateModelException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;

/**
 * <p>A hash model that wraps a resource bundle. Makes it convenient to store
 * localized content in the data model. It also acts as a method model that will
 * take a resource key and arbitrary number of arguments and will apply
 * {@link MessageFormat} with arguments on the string represented by the key.</p>
 *
 * <p>Typical usages:</p>
 * <ul>
 * <li><tt>bundle.resourceKey</tt> will retrieve the object from resource bundle
 * with key <tt>resourceKey</tt></li>
 * <li><tt>bundle("patternKey", arg1, arg2, arg3)</tt> will retrieve the string
 * from resource bundle with key <tt>patternKey</tt>, and will use it as a pattern
 * for MessageFormat with arguments arg1, arg2 and arg3</li>
 * </ul>
 */
public class ResourceBundleModel extends BeanModel implements TemplateFunctionModel {

    private Hashtable<String, MessageFormat> formats = null;

    public ResourceBundleModel(ResourceBundle bundle, DefaultObjectWrapper wrapper) {
        super(bundle, wrapper);
    }

    /**
     * Overridden to invoke the get method of the resource bundle.
     */
    @Override
    protected TemplateModel invokeGenericGet(Map keyMap, Class clazz, String key)
    throws TemplateModelException {
        try {
            return wrap(((ResourceBundle) object).getObject(key));
        } catch (MissingResourceException e) {
            throw new _TemplateModelException(e,
                    "No ", new _DelayedJQuote(key), " key in the ResourceBundle. "
                    + "Note that conforming to the ResourceBundle Java API, this is an error and not just "
                    + "a missing sub-variable (a null).");
        }
    }

    /**
     * Returns true if this bundle contains no objects.
     */
    @Override
    public boolean isEmpty() {
        return !((ResourceBundle) object).getKeys().hasMoreElements() &&
            super.isEmpty();
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    protected Set keySet() {
        Set set = super.keySet();
        Enumeration e = ((ResourceBundle) object).getKeys();
        while (e.hasMoreElements()) {
            set.add(e.nextElement());
        }
        return set;
    }

    /**
     * Takes first argument as a resource key, looks up a string in resource bundle
     * with this key, then applies a MessageFormat.format on the string with the
     * rest of the arguments. The created MessageFormats are cached for later reuse.
     */
    @Override
    public TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env) throws TemplateException {
        // Must have at least one argument - the key
        if (args.length < 1)
            throw new TemplateException("No message key was specified", env);
        // Read it
        String key = _CallableUtils.getStringArgument(args, 0, this);
        try {
            if (args.length == 1) {
                return wrap(((ResourceBundle) object).getObject(key));
            }

            // Copy remaining arguments into an Object[]
            int paramsLen = args.length - 1;
            Object[] params = new Object[paramsLen];
            for (int i = 0; i < paramsLen; ++i)
                params[i] = unwrap(args[1 + i]);

            // Invoke format
            return new BeanAndStringModel(format(key, params), wrapper);
        } catch (MissingResourceException e) {
            throw new TemplateModelException("No such key: " + key);
        } catch (Exception e) {
            throw new TemplateModelException(e.getMessage());
        }
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return null;
    }

    /**
     * Provides direct access to caching format engine from code (instead of from script).
     */
    public String format(String key, Object[] params)
        throws MissingResourceException {
        // Check to see if we already have a cache for message formats
        // and construct it if we don't
        // NOTE: this block statement should be synchronized. However
        // concurrent creation of two caches will have no harmful
        // consequences, and we avoid a performance hit.
        /* synchronized(this) */
        {
            if (formats == null)
                formats = new Hashtable();
        }

        MessageFormat format = null;
        // Check to see if we already have a requested MessageFormat cached
        // and construct it if we don't
        // NOTE: this block statement should be synchronized. However
        // concurrent creation of two formats will have no harmful
        // consequences, and we avoid a performance hit.
        /* synchronized(formats) */
        {
            format = (MessageFormat) formats.get(key);
            if (format == null) {
                format = new MessageFormat(((ResourceBundle) object).getString(key));
                format.setLocale(getBundle().getLocale());
                formats.put(key, format);
            }
        }

        // Perform the formatting. We synchronize on it in case it
        // contains date formatting, which is not thread-safe.
        synchronized (format) {
            return format.format(params);
        }
    }

    public ResourceBundle getBundle() {
        return (ResourceBundle) object;
    }
}
