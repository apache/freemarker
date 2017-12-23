package org.apache.freemarker.spring.model.form;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

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

public class InputTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    public static final String NAME = "input";

    private static final Map<String, String> ALLOWED_ATTRIBUTES = Collections.unmodifiableMap(
            createAttributeKeyNamePairsMap(
                    "size",
                    "maxlength",
                    "alt",
                    "onselect",
                    "readonly",
                    "autocomplete"
                    )
            );

    private static final int PATH_PARAM_IDX = 0;

    private static final ArgumentArrayLayout ARGS_LAYOUT =
            ArgumentArrayLayout.create(
                    1,
                    false,
                    null,
                    true
                    );

    protected InputTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    @Override
    public boolean isNestedContentSupported() {
        return false;
    }

    @Override
    public ArgumentArrayLayout getDirectiveArgumentArrayLayout() {
        return ARGS_LAYOUT;
    }

    @Override
    protected void executeInternal(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env,
            ObjectWrapperAndUnwrapper objectWrapperAndUnwrapper, RequestContext requestContext)
            throws TemplateException, IOException {

        final String path = CallableUtils.getStringArgument(args, PATH_PARAM_IDX, this);
        setAttributes(args);

        // TODO: convert value properly and write tag and attributes properly.
        out.write("<input");

        for (Map.Entry<String, Object> entry : getAttributes().entrySet()) {
            out.write(' ');
            out.write(entry.getKey());
            out.write("=\"");
            out.write(entry.getValue().toString());
            out.write('\"');
        }

        out.write("/>");
    }

    @Override
    protected boolean isAllowedAttribute(String localName, Object value) {
        return super.isAllowedAttribute(localName, value) && ALLOWED_ATTRIBUTES.containsKey(localName);
    }

}
