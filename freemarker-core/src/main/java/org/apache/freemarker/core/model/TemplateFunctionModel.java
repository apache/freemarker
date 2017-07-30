package org.apache.freemarker.core.model;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.ProcessingConfiguration;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.outputformat.OutputFormat;

/**
 * A {@link TemplateCallableModel}, which returns its result as a {@link TemplateModel} at the end of the execution.
 * This is in contrast with {@link TemplateDirectiveModel}, which writes its result progressively to the output.
 *
 * <p>Some template languages may allow function calls directly embedded into static text, as in
 * <code>text#f()text</code>. In that case, the language has to ensure that the return value is formatted according
 * the {@link ProcessingConfiguration} settings (such as {@link ProcessingConfiguration#getNumberFormat()} and
 * {@link ProcessingConfiguration#getDateFormat()}), and is printed to the output after escaping according the
 * {@link OutputFormat} of the context. Some template languages instead require using an explicit expression value
 * printer statement, as in <code>text${f()}text</code>.
 */
public interface TemplateFunctionModel extends TemplateCallableModel {

    TemplateModel execute(
            TemplateModel[] args, Environment env, CallPlace callPlace)
            throws TemplateException;

}
