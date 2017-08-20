package org.apache.freemarker.core.model;

import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.JavaMethodModel;
import org.apache.freemarker.core.util.CallableUtils;

/**
 * A {@link TemplateCallableModel}, which returns its result as a {@link TemplateModel} at the end of its execution.
 * This is in contrast with {@link TemplateDirectiveModel}, which writes its result progressively to the output, if it
 * has output at all. Also, {@link TemplateFunctionModel}-s can only be called where an expression is expected. (If
 * some future template languages allows calling functions outside expression context, on the top-level, then
 * that's a shorthand to doing that in with interpolation, like {@code ${f()}}.)
 * <p>
 * Example usage in templates: {@code < a href="${my.toProductURL(product.id)}">},
 * {@code <#list my.groupByFirstLetter(products, property="name") as productGroup>}
 * <p>
 * You can find utilities for implementing {@link TemplateFunctionModel}-s in {@link CallableUtils}.
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

    /**
     * Returns the argument array layout to use when calling the {@code {@link #execute(TemplateModel[], CallPlace,
     * Environment)}} method, or rarely {@code null}. If it's {@code null} then there can only be positional
     * arguments, any number of them (though of course the {@code execute} method implementation itself may restricts
     * the acceptable argument count), and the argument array will be simply as long as the number of arguments
     * specified at the call place. This layoutless mode is for example used by {@link JavaMethodModel}-s.
     */
    ArgumentArrayLayout getFunctionArgumentArrayLayout();

}
