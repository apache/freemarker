package org.apache.freemarker.core.model;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.CallPlace;
import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.NonTemplateCallPlace;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.impl.JavaMethodModel;
import org.apache.freemarker.core.util.CallableUtils;

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
 * <p>
 * You can find utilities for implementing {@link TemplateDirectiveModel}-s in {@link CallableUtils}.
 */
public interface TemplateDirectiveModel extends TemplateCallableModel {

    /**
     * Invokes the callable object.
     * <p>
     * This method shouldn't deliberately throw {@link RuntimeException}, nor {@link IOException} that wasn't caused by
     * writing to the output. Such exceptions should be catched inside the method and wrapped inside a
     * {@link TemplateException}. 
     *
     * @param args
     *         The array of argument values. Not {@code null}. If a parameter was omitted on the caller side, the
     *         corresponding array element will be {@code null}. The length of the array and the indexes correspond to
     *         the {@link ArgumentArrayLayout} returned by {@link #getDirectiveArgumentArrayLayout()}. {@link
     *         ArgumentArrayLayout} is not {@code null}, and the caller doesn't keep argument layout rules (such as the
     *         array is shorter than {@link ArgumentArrayLayout#getTotalLength()}, or the type of the values at {@link
     *         ArgumentArrayLayout#getPositionalVarargsArgumentIndex()} or at
     *         {@link ArgumentArrayLayout#getNamedVarargsArgumentIndex()}
     *         is improper), this method may throws {@link IndexOutOfBoundsException} or {@link ClassCastException}.
     *         Thus, user Java code that wishes to call {@link TemplateCallableModel}-s is responsible to ensure that
     *         the argument array follows the layout described by {@link ArgumentArrayLayout}, as the {@code execute}
     *         method isn't meant to do validations on that level.
     * @param callPlace
     *         The place (in a template, normally) where this directive was called from. Not {@code null}; in case the
     *         call is not from a template, you can use {@link NonTemplateCallPlace#INSTANCE} (or another {@link
     *         NonTemplateCallPlace} instance). Note that {@link CallPlace#executeNestedContent(TemplateModel[], Writer,
     *         Environment)} can be used to execute the nested content (even if there's no nested content; then simply
     *         nothing happens), and to pass nested content parameters to it.
     * @param out
     *         Print the output here (if there's any)
     * @param env
     *         The current processing environment. Not {@code null} in general, though certain implementations may
     *         specifically allow that, typically, implementations that are just adapters towards FreeMarker-unaware
     *         callables (for example, {@link JavaMethodModel} is like that).
     *
     * @throws TemplateException If any problem occurs that's not an {@link IOException} during writing the template
     *          output.
     * @throws IOException When writing the template output fails. Other {@link IOException}-s should be catched in this
     *          method and wrapped into {@link TemplateException}.
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

    /**
     * Returns the argument array layout to use when calling the {@code {@link #execute(TemplateModel[], CallPlace,
     * Writer, Environment)}} method, or rarely {@code null}. If it's {@code null} then there can only be positional
     * arguments, any number of them (though of course the {@code execute} method implementation itself may restricts
     * the acceptable argument count), and the argument array will be simply as long as the number of arguments
     * specified at the call place.
     */
    ArgumentArrayLayout getDirectiveArgumentArrayLayout();

}
