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
 * <p>
 * Note that {@link TemplateDirectiveModel} is a relatively low-level interface that puts more emphasis on
 * performance than on ease of implementation. TODO [FM3]: Introduce a more convenient way for implementing directives.
 */
public interface TemplateDirectiveModel extends TemplateCallableModel {

    /**
     * @param args
     *         The of argument values. Not {@code null}. If a parameter was omitted on the caller side, the
     *         corresponding array element will be {@code null}. For the indexed of arguments, see argument array layout
     *         in the {@link TemplateCallableModel} documentation.
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

    /**
     * Tells if this directive supports having nested content. If {@code false}, yet the caller specifies a non-empty
     * (strictly 0-length, not even whitespace is allowed), FreeMarker will throw a {@link TemplateException} with
     * descriptive error message, and {@link #execute(TemplateModel[], CallPlace, Writer, Environment)} won't be called.
     * If {@code true}, the author of the directive shouldn't forget calling {@link
     * CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)}, unless the intent was to skip the nested
     * content. (This property was added to prevent the frequent oversight (in FreeMarker 2) where a directive that
     * isn't supposed to have nested content doesn't examine if there's a nested content to throw an exception in that
     * case. Then if there's nested content, it will be silently skipped during execution, as the directive never
     * calls {@link CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)}.)
     */
    boolean isNestedContentSupported();

}
