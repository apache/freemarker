package org.apache.freemarker.core.model;

import java.io.IOException;
import java.io.Writer;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;

/**
 * A {@link TemplateCallableModel} that (progressively) prints it result into the {@code out} object, instead of
 * returning a single result at the end of the execution. Many of these won't print anything, but has other
 * side-effects why it's useful for calling them. When used in an expression context, the printer output will be the
 * value of the call (which depending on the output format of the directive is a {@link TemplateMarkupOutputModel},
 * or a {@link String}).
 */
// TODO [FM3][CF] Rename this to TemplateDirectiveModel
public interface TemplateDirectiveModel2 extends TemplateCallableModel {

    /**
     * @param args Array with {@link #getTotalArgumentCount()} elements (or more, in which case the extra elements
     *             should be ignored). If a parameter was omitted, the corresponding array element will be {@code null}.
     */
    void execute(
            TemplateModel[] args, Writer out,
            Environment env, CallPlace callPlace)
            throws TemplateException, IOException;

}
