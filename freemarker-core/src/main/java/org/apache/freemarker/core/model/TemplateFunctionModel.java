package org.apache.freemarker.core.model;

import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;

/**
 * A {@link TemplateCallableModel}, which returns its result as a {@link TemplateModel} at the end of its execution.
 * This is in contrast with {@link TemplateDirectiveModel}, which writes its result progressively to the output, if it
 * has output at all. Also, {@link TemplateFunctionModel}-s can only be called where an expression is expected. (If
 * some future template languages allows calling functions outside expression context, on the top-level, then
 * that's a shorthand to doing that in with interpolation, like {@code ${f()}}.)
 * <p>
 * Example usage in templates: {@code < a href="${my.toProductURL(product.id)}">},
 * {@code <#list my.groupByFirstLetter(products, property="name") as productGroup>}
 */
public interface TemplateFunctionModel extends TemplateCallableModel {

    /**
     * Invokes the function.
     *
     * @param args
     *         See the similar parameter of {@link TemplateDirectiveModel#execute(TemplateModel[], CallPlace, Writer,
     *         Environment)}
     * @param callPlace
     *         See the similar parameter of {@link TemplateDirectiveModel#execute(TemplateModel[], CallPlace, Writer,
     *         Environment)}
     * @param env
     *         See the similar parameter of {@link TemplateDirectiveModel#execute(TemplateModel[], CallPlace, Writer,
     *         Environment)}
     *
     * @return The return value of the function.
     */
    TemplateModel execute(TemplateModel[] args, CallPlace callPlace, Environment env) throws TemplateException;

}
