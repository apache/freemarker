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
     *         Array with {@link #getArgumentArraySize()} elements (or more, in which case the extra elements should be
     *         ignored). Not {@code null}. If a parameter was omitted on the caller side, the corresponding array
     *         element will be {@code null}. Parameters passed by position will be at the index that corresponds to the
     *         position (the 1st argument is at index 0). However, positional parameters over {@link
     *         #getPredefinedPositionalArgumentCount()} will be in the positional varargs sequence at index one higher,
     *         assuming {@link #hasPositionalVarargsArgument()} is {@code true}. Parameters passed by name (rather than
     *         by position) will be at the index returned be {@link #getPredefinedNamedArgumentIndex(String)}, or in the
     *         named varargs hash at index {@link #getNamedVarargsArgumentIndex()}, assuming that's not -1.
     * @param callPlace
     *         The place (in a template, normally) where this directive was called from. Not {@code null}. Note that
     *         {@link CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)} can be used to execute the
     *         nested content. If the directive doesn't support nested content, it should check {@link
     *         CallPlace#hasNestedContent()} that return {@code false}, and otherwise throw exception.
     * @param out
     *         Print the output here (if there's any)
     * @param env
     *         The current processing environment. Not {@code null}.
     *
     * @throws TemplateException
     *         If any problem occurs that's not an {@link IOException} during writing the template output.
     * @throws IOException
     *         When writing the template output fails.
     */
    void execute(TemplateModel[] args, CallPlace callPlace, Writer out, Environment env)
            throws TemplateException, IOException;

}
