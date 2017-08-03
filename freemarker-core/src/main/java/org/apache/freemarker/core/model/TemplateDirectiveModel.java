package org.apache.freemarker.core.model;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;

/**
 * A {@link TemplateCallableModel} that progressively writes it result into the {@code out} object, instead of
 * returning a single result at the end of the execution. Some directives won't print anything, and you call them from
 * other side-effects, or do flow control (conditional execution or repetition of the nested content).
 * <p>
 * They are not called from expression context, but on the top-level (that is, directly embedded into the
 * static text). (If some future template language allows calling them from expression context, the printed output
 * will be captured, and will be the return value of the call. Depending on the output format of the directive, the
 * type of that value will be {@link TemplateMarkupOutputModel} or {@link String}.)
 * <p>
 * Example usage in a template: {@code <@my.menu style="foo" expand=true>...</@my.menu>},
 * {@code <@my.menuItem "Some title" icon="some.jpg" />}.
 */
public interface TemplateDirectiveModel extends TemplateCallableModel {

    /**
     * Invokes the directive.
     *
     * @param args
     *         The of argument values. Not {@code null}. If a parameter was omitted on the caller side, the
     *         corresponding array element will be {@code null}. The length of the array and the indexes
     *         correspont to the {@link ArgumentArrayLayout} returned by {@link #getArgumentArrayLayout()}.
     *         If the caller doesn't keep argument layout rules (such as the array is shorter than
     *         {@link ArgumentArrayLayout#getTotalLength()}, or the type of the values at
     *         {@link ArgumentArrayLayout#getPositionalVarargsArgumentIndex()} or at
     *         {@link ArgumentArrayLayout#getNamedVarargsArgumentIndex()} is improper), this method may
     *         throws {@link IndexOutOfBoundsException} or {@link ClassCastException}. Thus, user Java code
     *         that wishes to call {@link TemplateCallableModel}-s is responsible to ensure that the argument array
     *         follows the layout described be {@link ArgumentArrayLayout}, as the {@code execute} method
     *         isn't meant to do validations on that level.
     * @param callPlace
     *         The place (in a template, normally) where this directive was called from. Not {@code null}. Note that
     *         {@link CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)} can be used to execute the
     *         nested content.
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
     * nested content (non-0-length, even whitespace matters), FreeMarker will throw a {@link TemplateException} with
     * descriptive error message, and {@link #execute(TemplateModel[], CallPlace, Writer, Environment)} won't be called.
     * If {@code true}, the author of the directive shouldn't forget calling {@link
     * CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)}, unless the intent was to skip the nested
     * content. (This property was added to prevent the frequent oversight (in FreeMarker 2) where a directive that
     * isn't supposed to have nested content doesn't examine if there's a nested content to throw an exception in that
     * case. Then if there's nested content, it will be silently skipped during execution, as the directive never calls
     * {@link CallPlace#executeNestedContent(TemplateModel[], Writer, Environment)}.)
     */
    boolean isNestedContentSupported();

}
