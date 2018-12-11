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

package org.apache.freemarker.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.DeepUnwrap;
import org.apache.freemarker.core.util.StringToIndexMap;


/**
 * A model that when invoked with a 'path' parameter will perform a servlet 
 * include. It also support an optional hash named 'params' which specifies
 * request parameters for the include - its keys are strings, its values
 * should be either strings or sequences of strings (for multiple valued 
 * parameters). A third optional parameter 'inherit_params' should be a boolean
 * when specified, and it defaults to true when not specified. A value of true
 * means that the include inherits the request parameters from the current 
 * request. In this case values in 'params' will get prepended to the existing
 * values of parameters.
 */
public class IncludePage implements TemplateDirectiveModel {

    private final HttpServletRequest request;
    private final HttpServletResponse response;

    private static final int PATH_PARAM_IDX = 0;
    private static final int INHERIT_PARAMS_PARAM_IDX = 1;
    private static final int PARAMS_PARAM_IDX = 2;

    private static final String PATH_PARAM_NAME = "path";
    private static final String INHERIT_PARAMS_PARAM_NAME = "inherit_params";
    private static final String PARAMS_PARAM_NAME = "params";

    private static final ArgumentArrayLayout ARGS_LAYOUT = ArgumentArrayLayout.create(
            0,
            false,
            StringToIndexMap.of(
                    PATH_PARAM_NAME, PATH_PARAM_IDX,
                    INHERIT_PARAMS_PARAM_NAME, INHERIT_PARAMS_PARAM_IDX
            ),
            false
    );
    
    public IncludePage(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException {
        final String strPath = CallableUtils.getStringArgument(args, PARAMS_PARAM_IDX, this);

        // See whether we need to use a custom response (if we're inside a TTM
        // or TDM or macro nested body, we'll need to as then the current 
        // FM environment writer is not identical to HTTP servlet response 
        // writer. 
        final Writer envOut = env.getOut(); 
        final HttpServletResponse wrappedResponse;
        if (envOut == response.getWriter()) {
            // Don't bother wrapping if environment's writer is same as 
            // response writer
            wrappedResponse = response;
        } else {
            final PrintWriter printWriter = (envOut instanceof PrintWriter) ?
                (PrintWriter) envOut :
                new PrintWriter(envOut); 
            // Otherwise, invoke a response wrapper that will pass the
            // env writer, potentially first wrapping it in a print
            // writer when it ain't one already.
            wrappedResponse = new HttpServletResponseWrapper(response) {
                @Override
                public PrintWriter getWriter() {
                    return printWriter;
                }
            };
        }

        // Determine inherit_params value
        final boolean inheritParams;
        final TemplateModel inheritParamsModel = args[INHERIT_PARAMS_PARAM_IDX];
        if (inheritParamsModel == null) {
            // defaults to true when not specified
            inheritParams = true; 
        } else {
            if (!(inheritParamsModel instanceof TemplateBooleanModel)) {
                throw new TemplateException(env,
                        "\"", INHERIT_PARAMS_PARAM_NAME, "\" should be a boolean but it's a(n) ",
                        inheritParamsModel.getClass().getName(), " instead");
            }
            inheritParams = ((TemplateBooleanModel) inheritParamsModel).getAsBoolean();
        }
        
        // Get explicit params, if any
        final TemplateModel paramsModel = args[PARAMS_PARAM_IDX];
        
        // Determine whether we need to wrap the request
        final HttpServletRequest wrappedRequest;
        if (paramsModel == null && inheritParams) {
            // Inherit original request params & no params explicitly 
            // specified, so use the original request
            wrappedRequest = request;
        } else {
            // In any other case, use a custom request wrapper
            final Map paramsMap;
            if (paramsModel != null) {
                // Convert params to a Map
                final Object unwrapped = DeepUnwrap.unwrap(paramsModel);
                if (!(unwrapped instanceof Map)) {
                    throw new TemplateException(env,
                            "Expected \"", PARAMS_PARAM_NAME, "\" to unwrap into a java.util.Map. It unwrapped into ",
                            unwrapped.getClass().getName(), " instead.");
                }
                paramsMap = (Map) unwrapped;
            } else {
                paramsMap = Collections.EMPTY_MAP;
            }
            wrappedRequest = new CustomParamsRequest(request, paramsMap, 
                    inheritParams);
        }
        
        // Finally, do the include
        try {
            request.getRequestDispatcher(strPath).include(wrappedRequest, 
                    wrappedResponse);
        } catch (ServletException e) {
            throw new TemplateException(e, env);
        }
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

    private static final class CustomParamsRequest extends HttpServletRequestWrapper {
        private final HashMap paramsMap;

        private CustomParamsRequest(HttpServletRequest request, Map paramMap, 
                boolean inheritParams) {
            super(request);
            paramsMap = inheritParams ? new HashMap(request.getParameterMap()) : new HashMap();
            for (Iterator it = paramMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                String name = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                final String[] valueArray;
                if (value == null) {
                    // Null values are explicitly added (so, among other 
                    // things, we can hide inherited param values).
                    valueArray = new String[] { null };
                } else if (value instanceof String[]) {
                    // String[] arrays are just passed through
                    valueArray = (String[]) value;
                } else if (value instanceof Collection) {
                    // Collections are converted to String[], with 
                    // String.valueOf() used on elements
                    Collection col = (Collection) value;
                    valueArray = new String[col.size()];
                    int i = 0;
                    for (Iterator it2 = col.iterator(); it2.hasNext(); ) {
                        valueArray[i++] = String.valueOf(it2.next());
                    }
                } else if (value.getClass().isArray()) {
                    // Other array types are too converted to String[], with 
                    // String.valueOf() used on elements
                    int len = Array.getLength(value);
                    valueArray = new String[len];
                    for (int i = 0; i < len; ++i) {
                        valueArray[i] = String.valueOf(Array.get(value, i));
                    }
                } else {
                    // All other values (including strings) are converted to a
                    // single-element String[], with String.valueOf applied to
                    // the value.
                    valueArray = new String[] { String.valueOf(value) };
                }
                String[] existingParams = (String[]) paramsMap.get(name);
                int el = existingParams == null ? 0 : existingParams.length;
                if (el == 0) {
                    // No original params, just put our array
                    paramsMap.put(name, valueArray);
                } else {
                    int vl = valueArray.length;
                    if (vl > 0) {
                        // Both original params and new params, prepend our
                        // params to original params
                        String[] newValueArray = new String[el + vl];
                        System.arraycopy(valueArray, 0, newValueArray, 0, vl);
                        System.arraycopy(existingParams, 0, newValueArray, vl, el);
                        paramsMap.put(name, newValueArray);
                    }
                }
            }
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] value = ((String[]) paramsMap.get(name));
            return value != null ? value.clone() : null;
        }

        @Override
        public String getParameter(String name) {
            String[] values = (String[]) paramsMap.get(name);
            return values != null && values.length > 0 ? values[0] : null;
        }

        @Override
        public Enumeration getParameterNames() {
            return Collections.enumeration(paramsMap.keySet());
        }

        @Override
        public Map getParameterMap() {
            HashMap clone = (HashMap) paramsMap.clone();
            for (Iterator it = clone.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                entry.setValue(((String[]) entry.getValue()).clone());
            }
            return Collections.unmodifiableMap(clone);
        }
    }
}
