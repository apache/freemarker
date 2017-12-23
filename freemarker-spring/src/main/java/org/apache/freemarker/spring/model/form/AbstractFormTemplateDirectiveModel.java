package org.apache.freemarker.spring.model.form;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.freemarker.spring.model.AbstractSpringTemplateDirectiveModel;

/**
 * Corresponds to <code>org.springframework.web.servlet.tags.form.AbstractFormTag</code>.
 */
public abstract class AbstractFormTemplateDirectiveModel extends AbstractSpringTemplateDirectiveModel {

    protected AbstractFormTemplateDirectiveModel(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

}
