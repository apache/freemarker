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

package org.apache.freemarker.spring.model;

import java.beans.PropertyEditor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.springframework.web.servlet.support.RequestContext;

/**
 * A <code>TemplateFunctionModel</code> providing functionality equivalent to the Spring Framework's
 * <code>&lt;spring:transform /&gt;</code> JSP Tag Library.
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 * &lt;@spring.bind "user.birthDate"; status&gt;
 *   &lt;div id="userBirthDate"&gt;${spring.transform(status.editor, status.actualValue)}&lt;/div&gt;
 * &lt;/@spring.bind&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;spring:bind /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always has <code>BindStatus</code> not to escape HTML's
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */
class TransformFunction extends AbstractSpringTemplateFunctionModel {

    public static final String NAME = "transform";

    private static final int PROPERTY_EDITOR_PARAM_IDX = 0;
    private static final int VALUE_PARAM_IDX = 1;

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    2,
                    false,
                    null,
                    false
                    );

    protected TransformFunction(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected TemplateModel executeInternal(TemplateModel[] args, CallPlace callPlace, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
                    throws TemplateException {
        final TemplateModel editorModel = CallableUtils.getOptionalArgument(args, PROPERTY_EDITOR_PARAM_IDX,
                TemplateModel.class, this);
        final PropertyEditor editor = (editorModel != null)
                ? (PropertyEditor) objectWrapperAndUnwrapper.unwrap(editorModel) : null;

        final TemplateModel valueModel = CallableUtils.getOptionalArgument(args, VALUE_PARAM_IDX, TemplateModel.class,
                this);
        final Object value = (valueModel != null) ? objectWrapperAndUnwrapper.unwrap(valueModel) : null;

        String valueAsString = null;

        if (value != null) {
            if (editor != null) {
                editor.setValue(value);
                valueAsString = editor.getAsText();
            } else {
                valueAsString = value.toString();
            }
        }

        return (valueAsString != null) ? objectWrapperAndUnwrapper.wrap(valueAsString) : null;
    }

    @Override
    public ArgumentArrayLayout getFunctionArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

}
