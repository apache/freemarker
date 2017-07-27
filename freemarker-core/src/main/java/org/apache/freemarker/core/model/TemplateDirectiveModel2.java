package org.apache.freemarker.core.model;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;

/**
 * A {@link TemplateCallableModel} that (progressively) prints it result into the {@code out} object, instead of
 * returning a single result at the end of the execution. Many of these won't print anything, but has other
 * side-effects why it's useful for calling them, or do flow control. They are used in templates like
 * {@code <@myDirective foo=1 bar="wombat">...</@myDirective>} (or as {@code <@myDirective foo=1 bar="wombat" />} -
 * the nested content is optional).
 * <p>
 * When called from expression context (and if the template language allows that!), the printed output will be captured,
 * and will be the return value of the call. Depending on the output format of the directive, the type of that value
 * will be {@link TemplateMarkupOutputModel} or {@link String}.
 */
// TODO [FM3][CF] Rename this to TemplateDirectiveModel
public interface TemplateDirectiveModel2 extends TemplateCallableModel {

    /**
     * @param args
     *         Array with {@link #getTotalArgumentCount()} elements (or more, in which case the extra elements should be
     *         ignored). Not {@code null}. If a parameter was omitted, the corresponding array element will be {@code
     *         null}.
     * @param callPlace
     *         The place (in a template, normally) where this directive was called from. Not {@code null}. Note that
     *         {@link CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)} can be used to
     * @param out
     *         Print the output here (if there's any)
     * @param env
     *         The current processing environment. Not {@code null}.
     */
    void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException;

}
