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
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.ArgumentArrayLayout;
import org.apache.freemarker.core.model.ObjectWrapperAndUnwrapper;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util.StringToIndexMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.BindStatus;
import org.springframework.web.servlet.support.RequestContext;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractHtmlElementTag</code>.
 */
abstract class AbstractHtmlElementTemplateDirectiveModel
        extends AbstractDataBoundFormElementTemplateDirectiveModel {

    private static final int NAMED_ARGS_OFFSET = AbstractDataBoundFormElementTemplateDirectiveModel.ARGS_LAYOUT
            .getPredefinedNamedArgumentsEndIndex();

    private static final int CSS_CLASS_PARAM_IDX = NAMED_ARGS_OFFSET;
    private static final String CSS_CLASS_PARAM_NAME = "cssClass";
    private static final String CSS_CLASS_ATTR_NAME = "class";

    private static final int CSS_STYLE_PARAM_IDX = NAMED_ARGS_OFFSET + 1;
    private static final String CSS_STYLE_PARAM_NAME = "cssStyle";
    private static final String CSS_STYLE_ATTR_NAME = "style";

    private static final int LANG_PARAM_IDX = NAMED_ARGS_OFFSET + 2;
    private static final String LANG_PARAM_NAME = "lang";

    private static final int TITLE_PARAM_IDX = NAMED_ARGS_OFFSET + 3;
    private static final String TITLE_PARAM_NAME = "title";

    private static final int DIR_PARAM_IDX = NAMED_ARGS_OFFSET + 4;
    private static final String DIR_PARAM_NAME = "dir";

    private static final int TABINDEX_PARAM_IDX = NAMED_ARGS_OFFSET + 5;
    private static final String TABINDEX_PARAM_NAME = "tabindex";

    private static final int ONCLICK_PARAM_IDX = NAMED_ARGS_OFFSET + 6;
    private static final String ONCLICK_PARAM_NAME = "onclick";

    private static final int ONDBLCLICK_PARAM_IDX = NAMED_ARGS_OFFSET + 7;
    private static final String ONDBLCLICK_PARAM_NAME = "ondblclick";

    private static final int ONMOUSEDOWN_PARAM_IDX = NAMED_ARGS_OFFSET + 8;
    private static final String ONMOUSEDOWN_PARAM_NAME = "onmousedown";

    private static final int ONMOUSEUP_PARAM_IDX = NAMED_ARGS_OFFSET + 9;
    private static final String ONMOUSEUP_PARAM_NAME = "onmouseup";

    private static final int ONMOUSEOVER_PARAM_IDX = NAMED_ARGS_OFFSET + 10;
    private static final String ONMOUSEOVER_PARAM_NAME = "onmouseover";

    private static final int ONMOUSEMOVE_PARAM_IDX = NAMED_ARGS_OFFSET + 11;
    private static final String ONMOUSEMOVE_PARAM_NAME = "onmousemove";

    private static final int ONMOUSEOUT_PARAM_IDX = NAMED_ARGS_OFFSET + 12;
    private static final String ONMOUSEOUT_PARAM_NAME = "onmouseout";

    private static final int ONKEYPRESS_PARAM_IDX = NAMED_ARGS_OFFSET + 13;
    private static final String ONKEYPRESS_PARAM_NAME = "onkeypress";

    private static final int ONKEYUP_PARAM_IDX = NAMED_ARGS_OFFSET + 14;
    private static final String ONKEYUP_PARAM_NAME = "onkeyup";

    private static final int ONKEYDOWN_PARAM_IDX = NAMED_ARGS_OFFSET + 15;
    private static final String ONKEYDOWN_PARAM_NAME = "onkeydown";

    private static final int CSSERRORCLASS_PARAM_IDX = NAMED_ARGS_OFFSET + 16;
    private static final String CSSERRORCLASS_PARAM_NAME = "cssErrorClass";

    protected static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    StringToIndexMap.of(AbstractDataBoundFormElementTemplateDirectiveModel.ARGS_LAYOUT.getPredefinedNamedArgumentsMap(),
                            new StringToIndexMap.Entry(CSS_CLASS_PARAM_NAME, CSS_CLASS_PARAM_IDX),
                            new StringToIndexMap.Entry(CSS_STYLE_PARAM_NAME, CSS_STYLE_PARAM_IDX),
                            new StringToIndexMap.Entry(LANG_PARAM_NAME, LANG_PARAM_IDX),
                            new StringToIndexMap.Entry(TITLE_PARAM_NAME, TITLE_PARAM_IDX),
                            new StringToIndexMap.Entry(DIR_PARAM_NAME, DIR_PARAM_IDX),
                            new StringToIndexMap.Entry(TABINDEX_PARAM_NAME, TABINDEX_PARAM_IDX),
                            new StringToIndexMap.Entry(ONCLICK_PARAM_NAME, ONCLICK_PARAM_IDX),
                            new StringToIndexMap.Entry(ONDBLCLICK_PARAM_NAME, ONDBLCLICK_PARAM_IDX),
                            new StringToIndexMap.Entry(ONMOUSEDOWN_PARAM_NAME, ONMOUSEDOWN_PARAM_IDX),
                            new StringToIndexMap.Entry(ONMOUSEUP_PARAM_NAME, ONMOUSEUP_PARAM_IDX),
                            new StringToIndexMap.Entry(ONMOUSEOVER_PARAM_NAME, ONMOUSEOVER_PARAM_IDX),
                            new StringToIndexMap.Entry(ONMOUSEMOVE_PARAM_NAME, ONMOUSEMOVE_PARAM_IDX),
                            new StringToIndexMap.Entry(ONMOUSEOUT_PARAM_NAME, ONMOUSEOUT_PARAM_IDX),
                            new StringToIndexMap.Entry(ONKEYPRESS_PARAM_NAME, ONKEYPRESS_PARAM_IDX),
                            new StringToIndexMap.Entry(ONKEYUP_PARAM_NAME, ONKEYUP_PARAM_IDX),
                            new StringToIndexMap.Entry(ONKEYDOWN_PARAM_NAME, ONKEYDOWN_PARAM_IDX),
                            new StringToIndexMap.Entry(CSSERRORCLASS_PARAM_NAME, CSSERRORCLASS_PARAM_IDX)
                            ),
                    true
                    );

    private String cssClass;
    private String cssStyle;
    private String lang;
    private String title;
    private String dir;
    private String tabindex;
    private String onclick;
    private String ondblclick;
    private String onmousedown;
    private String onmouseup;
    private String onmouseover;
    private String onmousemove;
    private String onmouseout;
    private String onkeypress;
    private String onkeyup;
    private String onkeydown;
    private String cssErrorClass;

    private Map<String, Object> dynamicAttributes;
    private Map<String, Object> unmodifiableDynamicAttributes = Collections.emptyMap();

    protected AbstractHtmlElementTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {

        super.executeInternal(args, callPlace, out, env, objectWrapperAndUnwrapper, requestContext);

        cssClass = CallableUtils.getOptionalStringArgument(args, CSS_CLASS_PARAM_IDX, this);
        cssStyle = CallableUtils.getOptionalStringArgument(args, CSS_STYLE_PARAM_IDX, this);
        lang = CallableUtils.getOptionalStringArgument(args, LANG_PARAM_IDX, this);
        title = CallableUtils.getOptionalStringArgument(args, TITLE_PARAM_IDX, this);
        dir = CallableUtils.getOptionalStringArgument(args, DIR_PARAM_IDX, this);
        tabindex = CallableUtils.getOptionalStringArgument(args, TABINDEX_PARAM_IDX, this);
        onclick = CallableUtils.getOptionalStringArgument(args, ONCLICK_PARAM_IDX, this);
        ondblclick = CallableUtils.getOptionalStringArgument(args, ONDBLCLICK_PARAM_IDX, this);
        onmousedown = CallableUtils.getOptionalStringArgument(args, ONMOUSEDOWN_PARAM_IDX, this);
        onmouseup = CallableUtils.getOptionalStringArgument(args, ONMOUSEUP_PARAM_IDX, this);
        onmouseover = CallableUtils.getOptionalStringArgument(args, ONMOUSEOVER_PARAM_IDX, this);
        onmousemove = CallableUtils.getOptionalStringArgument(args, ONMOUSEMOVE_PARAM_IDX, this);
        onmouseout = CallableUtils.getOptionalStringArgument(args, ONMOUSEOUT_PARAM_IDX, this);
        onkeypress = CallableUtils.getOptionalStringArgument(args, ONKEYPRESS_PARAM_IDX, this);
        onkeyup = CallableUtils.getOptionalStringArgument(args, ONKEYUP_PARAM_IDX, this);
        onkeydown = CallableUtils.getOptionalStringArgument(args, ONKEYDOWN_PARAM_IDX, this);
        cssErrorClass = CallableUtils.getOptionalStringArgument(args, CSSERRORCLASS_PARAM_IDX, this);

        final int attrsVarargsIndex = getDirectiveArgumentArrayLayout().getNamedVarargsArgumentIndex();
        final TemplateHashModelEx attrsHashModel = (TemplateHashModelEx) args[attrsVarargsIndex];

        if (attrsHashModel != null && !attrsHashModel.isEmptyHash()) {
            for (TemplateHashModelEx.KeyValuePairIterator attrIt = attrsHashModel.keyValuePairIterator(); attrIt.hasNext();) {
                TemplateHashModelEx.KeyValuePair pair = attrIt.next();
                TemplateModel attrNameModel = pair.getKey();
                TemplateModel attrValueModel = pair.getValue();

                if (!(attrNameModel instanceof TemplateStringModel)) {
                    throw CallableUtils.newArgumentValueException(attrsVarargsIndex,
                            "Attribute name must be a string.", this);
                }

                String attrName = ((TemplateStringModel) attrNameModel).getAsString();

                if (attrName.isEmpty()) {
                    throw CallableUtils.newArgumentValueException(attrsVarargsIndex,
                            "Attribute name must be a non-blank string.", this);
                }

                final Object attrValue = objectWrapperAndUnwrapper.unwrap(attrValueModel);
                setDynamicAttribute(attrName, attrValue);
            }
        }
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getCssStyle() {
        return cssStyle;
    }

    public String getLang() {
        return lang;
    }

    public String getTitle() {
        return title;
    }

    public String getDir() {
        return dir;
    }

    public String getTabindex() {
        return tabindex;
    }

    public String getOnclick() {
        return onclick;
    }

    public String getOndblclick() {
        return ondblclick;
    }

    public String getOnmousedown() {
        return onmousedown;
    }

    public String getOnmouseup() {
        return onmouseup;
    }

    public String getOnmouseover() {
        return onmouseover;
    }

    public String getOnmousemove() {
        return onmousemove;
    }

    public String getOnmouseout() {
        return onmouseout;
    }

    public String getOnkeypress() {
        return onkeypress;
    }

    public String getOnkeyup() {
        return onkeyup;
    }

    public String getOnkeydown() {
        return onkeydown;
    }

    public String getCssErrorClass() {
        return cssErrorClass;
    }

    public Map<String, Object> getDynamicAttributes() {
        return unmodifiableDynamicAttributes;
    }

    private void setDynamicAttribute(String localName, Object value) {
        if (localName == null) {
            throw new IllegalArgumentException("Attribute name must not be null.");
        }

        if (!isValidDynamicAttribute(localName, value)) {
            throw new IllegalArgumentException("Invalid dynamic attribute: " + localName + "=" + value);
        }

        if (dynamicAttributes == null) {
            dynamicAttributes = new LinkedHashMap<String, Object>();
            unmodifiableDynamicAttributes = Collections.unmodifiableMap(dynamicAttributes);
        }

        dynamicAttributes.put(localName, value);
    }

    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return true;
    }

    protected void writeDefaultAttributes(TagOutputter tagOut) throws TemplateException, IOException {
        super.writeDefaultAttributes(tagOut);
        writeOptionalAttributes(tagOut);
    }

    protected void writeOptionalAttributes(TagOutputter tagOut) throws TemplateException, IOException {
        tagOut.writeOptionalAttributeValue(CSS_CLASS_ATTR_NAME, resolveCssClass());
        tagOut.writeOptionalAttributeValue(CSS_STYLE_ATTR_NAME,
                ObjectUtils.getDisplayString(evaluate(CSS_STYLE_PARAM_NAME, getCssStyle())));
        writeOptionalAttribute(tagOut, LANG_PARAM_NAME, getLang());
        writeOptionalAttribute(tagOut, TITLE_PARAM_NAME, getTitle());
        writeOptionalAttribute(tagOut, DIR_PARAM_NAME, getDir());
        writeOptionalAttribute(tagOut, TABINDEX_PARAM_NAME, getTabindex());
        writeOptionalAttribute(tagOut, ONCLICK_PARAM_NAME, getOnclick());
        writeOptionalAttribute(tagOut, ONDBLCLICK_PARAM_NAME, getOndblclick());
        writeOptionalAttribute(tagOut, ONMOUSEDOWN_PARAM_NAME, getOnmousedown());
        writeOptionalAttribute(tagOut, ONMOUSEUP_PARAM_NAME, getOnmouseup());
        writeOptionalAttribute(tagOut, ONMOUSEOVER_PARAM_NAME, getOnmouseover());
        writeOptionalAttribute(tagOut, ONMOUSEMOVE_PARAM_NAME, getOnmousemove());
        writeOptionalAttribute(tagOut, ONMOUSEOUT_PARAM_NAME, getOnmouseout());
        writeOptionalAttribute(tagOut, ONKEYPRESS_PARAM_NAME, getOnkeypress());
        writeOptionalAttribute(tagOut, ONKEYUP_PARAM_NAME, getOnkeyup());
        writeOptionalAttribute(tagOut, ONKEYDOWN_PARAM_NAME, getOnkeydown());

        if (!unmodifiableDynamicAttributes.isEmpty()) {
            for (String attr : unmodifiableDynamicAttributes.keySet()) {
                tagOut.writeOptionalAttributeValue(attr, getDisplayString(unmodifiableDynamicAttributes.get(attr)));
            }
        }
    }

    protected String resolveCssClass() throws TemplateException {
        final BindStatus bindStatus = getBindStatus();

        if (bindStatus != null && bindStatus.isError() && StringUtils.hasText(getCssErrorClass())) {
            return ObjectUtils.getDisplayString(evaluate("cssErrorClass", getCssErrorClass()));
        } else {
            return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
        }
    }

}
