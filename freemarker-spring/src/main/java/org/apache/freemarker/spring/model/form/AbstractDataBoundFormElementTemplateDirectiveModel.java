package org.apache.freemarker.spring.model.form;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractDataBoundFormElementTag</code>.
 */
public abstract class AbstractDataBoundFormElementTemplateDirectiveModel extends AbstractFormTemplateDirectiveModel {

    protected AbstractDataBoundFormElementTemplateDirectiveModel(HttpServletRequest request,
            HttpServletResponse response) {
        super(request, response);
    }

}
