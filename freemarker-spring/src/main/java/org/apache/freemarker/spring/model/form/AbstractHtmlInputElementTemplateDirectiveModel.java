package org.apache.freemarker.spring.model.form;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractHtmlInputElementTemplateDirectiveModel extends AbstractHtmlElementTemplateDirectiveModel {

    protected AbstractHtmlInputElementTemplateDirectiveModel(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

}
