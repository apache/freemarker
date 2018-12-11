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

package org.apache.freemarker.spring.model.form;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.springframework.web.bind.WebDataBinder;

/**
 * Provides <code>TemplateModel</code> for data-binding-aware multiple HTML '{@code <input type="checkbox"/>}' elements.
 * This tag is provided for completeness if the application relies on a
 * <code>org.springframework.web.servlet.support.RequestDataValueProcessor</code>.
 * <P>
 * This directive supports the following parameters:
 * <UL>
 * <LI>
 *   ... TODO ...
 * </LI>
 * </UL>
 * </P>
 * <P>
 * Some valid example(s):
 * </P>
 * <PRE>
 *   &lt;#assign foodItems = [ "Sandwich", "Spaghetti", "Sushi" ] &gt;
 *   &lt;@form.checkboxes "user.favoriteFood" items=foodItems /&gt;
 * </PRE>
 * Or,
 * <PRE>
 *   &lt;#assign foodItemHash = { "Sandwich": "Delicious sandwich", "Spaghetti": "Lovely spaghetti", "Sushi": "Sushi with wasabi" } &gt;
 *   &lt;@form.checkboxes "user.favoriteFood" items=foodItemHash /&gt;
 * </PRE>
 * <P>
 * <EM>Note:</EM> Unlike Spring Framework's <code>&lt;form:button /&gt;</code> JSP Tag Library, this directive
 * does not support <code>htmlEscape</code> parameter. It always renders HTML's without escaping
 * because it is much easier to control escaping in FreeMarker Template expressions.
 * </P>
 */

class CheckboxesTemplateDirectiveModel extends AbstractMultiCheckedElementTemplateDirectiveModel {

    public static final String NAME = "checkboxes";

    protected CheckboxesTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    protected void writeAdditionalDetails(Environment env, TagOutputter tagOut) throws TemplateException, IOException {
        if (!isDisabled()) {
            // Spring Web MVC requires to render a hidden input as a 'field was present' marker.
            // Write out the 'field was present' marker.
            tagOut.beginTag("input");
            tagOut.writeAttribute("type", "hidden");
            String name = WebDataBinder.DEFAULT_FIELD_MARKER_PREFIX + getName();
            tagOut.writeAttribute("name", name);
            tagOut.writeAttribute("value", processFieldValue(env, name, "on", getInputType()));
            tagOut.endTag();
        }
    }

    @Override
    protected String getInputType() {
        return "checkbox";
    }

}
