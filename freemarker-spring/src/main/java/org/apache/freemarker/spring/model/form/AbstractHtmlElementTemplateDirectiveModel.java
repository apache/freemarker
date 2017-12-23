package org.apache.freemarker.spring.model.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateBooleanModel;
import org.apache.freemarker.core.model.TemplateHashModelEx;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.TemplateStringModel;
import org.apache.freemarker.core.util.CallableUtils;
import org.apache.freemarker.core.util._KeyValuePair;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractHtmlElementTag</code>.
 */
public abstract class AbstractHtmlElementTemplateDirectiveModel
        extends AbstractDataBoundFormElementTemplateDirectiveModel {

    protected static Map<String, String> createAttributeKeyNamePairsMap(String ... attrNames) {
        Map<String, String> map = new HashMap<>();
        for (String attrName : attrNames) {
            map.put(attrName.toUpperCase(), attrName);
        }
        return map;
    }

    private static final Map<String, String> ALLOWED_ATTRIBUTES = Collections.unmodifiableMap(
            createAttributeKeyNamePairsMap(
                    "class",
                    "style",
                    "lang",
                    "title",
                    "dir",
                    "tabindex",
                    "onclick",
                    "ondblclick",
                    "onmousedown",
                    "onmouseup",
                    "onmouseover",
                    "onmousemove",
                    "onmouseout",
                    "onkeypress",
                    "onkeyup",
                    "onkeydown")
            );

    private Map<String, Object> attributes;
    private Map<String, Object> unmodifiableAttributes = Collections.emptyMap();

    protected AbstractHtmlElementTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    public Map<String, Object> getAttributes() {
        return unmodifiableAttributes;
    }

    public void setAttribute(String localName, Object value) {
        if (localName == null) {
            throw new IllegalArgumentException("Attribute name must not be null.");
        }

        if (!isValidDynamicAttribute(localName, value)) {
            throw new IllegalArgumentException("Invalid attribute: " + localName + "=" + value);
        }

        if (attributes == null) {
            attributes = new LinkedHashMap<String, Object>();
            unmodifiableAttributes = Collections.unmodifiableMap(attributes);
        }

        attributes.put(localName, value);
    }

    protected boolean isAllowedAttribute(String localName, Object value) {
        return ALLOWED_ATTRIBUTES.containsKey(localName.toUpperCase());
    }

    protected boolean isValidDynamicAttribute(String localName, Object value) {
        return true;
    }

    protected void setAttributes(TemplateModel[] args) throws TemplateException {
        final int attrsVarargsIndex = getDirectiveArgumentArrayLayout().getNamedVarargsArgumentIndex();
        final TemplateHashModelEx attrsHashModel = (TemplateHashModelEx) args[attrsVarargsIndex];

        List<_KeyValuePair<String, String>> attrs = Collections.emptyList();

        if (!attrsHashModel.isEmptyHash()) {
            attrs = new ArrayList<>();

            for (TemplateHashModelEx.KeyValuePairIterator attrIt = attrsHashModel.keyValuePairIterator(); attrIt.hasNext();) {
                TemplateHashModelEx.KeyValuePair pair = attrIt.next();
                TemplateModel attrNameModel = pair.getKey();
                TemplateModel attrValueModel = pair.getValue();

                if (!(attrNameModel instanceof TemplateStringModel)) {
                    throw CallableUtils.newArgumentValueException(attrsVarargsIndex,
                            "Parameter name must be a string.", this);
                }

                String attrName = ((TemplateStringModel) attrNameModel).getAsString();

                if (attrName.isEmpty()) {
                    throw CallableUtils.newArgumentValueException(attrsVarargsIndex,
                            "Attribute name must be a non-blank string.", this);
                }

                // TODO: Don't assume attribute value is string. Treat it as object and convert properly by using Spring utils.

                String attrValue;

                if (attrValueModel instanceof TemplateStringModel) {
                    attrValue = ((TemplateStringModel) attrValueModel).getAsString();
                } else if (attrValueModel instanceof TemplateNumberModel) {
                    attrValue = ((TemplateNumberModel) attrValueModel).getAsNumber().toString();
                } else if (attrValueModel instanceof TemplateBooleanModel) {
                    attrValue = Boolean.toString(((TemplateBooleanModel) attrValueModel).getAsBoolean());
                } else {
                    throw CallableUtils.newArgumentValueException(attrsVarargsIndex,
                            "Format the attribute manually to properly coerce it to a URL parameter value string. "
                                    + "e.g, date?string.iso, date?long, list?join('_'), etc.",
                            this);
                }

                setAttribute(attrName, attrValue);
            }
        }
    }
}
