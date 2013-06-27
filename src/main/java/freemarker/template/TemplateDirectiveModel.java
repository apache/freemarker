package freemarker.template;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.utility.DeepUnwrap;

/**
 * "directive" template language data type: used as user-defined directives 
 * (much like macros) in templates. They can do arbitrary actions, write arbitrary
 * text to the template output, and trigger rendering of their nested content for
 * any number of times.
 * 
 * <p>They are used in templates like {@code <@myDirective foo=1 bar="wombat">...</@myDirective>} (or as
 * {@code <@myDirective foo=1 bar="wombat" />} - the nested content is optional).
 *
 * @since 2.3.11
 * @author Attila Szegedi
 */
public interface TemplateDirectiveModel extends TemplateModel
{
    /**
     * Executes this user-defined directive; called by FreeMarker when the user-defined
     * directive is called in the template.
     *
     * @param env the current processing environment. Note that you can access
     * the output {@link java.io.Writer Writer} by {@link Environment#getOut()}.
     * @param params the parameters (if any) passed to the directive as a 
     * map of key/value pairs where the keys are {@link String}-s and the 
     * values are {@link TemplateModel} instances. This is never 
     * <code>null</code>. If you need to convert the template models to POJOs,
     * you can use the utility methods in the {@link DeepUnwrap} class.
     * @param loopVars an array that corresponds to the "loop variables", in
     * the order as they appear in the directive call. ("Loop variables" are out-parameters
     * that are available to the nested body of the directive; see in the Manual.)
     * You set the loop variables by writing this array. The length of the array gives the
     * number of loop-variables that the caller has specified.
     * Never <code>null</code>, but can be a zero-length array.
     * @param body an object that can be used to render the nested content (body) of
     * the directive call. If the directive call has no nested content (i.e., it's like
     * [@myDirective /] or [@myDirective][/@myDirective]), then this will be
     * <code>null</code>.
     *
     * @throws TemplateException
     * @throws IOException
     */
   public void execute(Environment env, Map params, TemplateModel[] loopVars, 
            TemplateDirectiveBody body) throws TemplateException, IOException;
}