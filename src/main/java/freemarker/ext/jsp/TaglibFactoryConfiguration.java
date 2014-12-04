package freemarker.ext.jsp;

import java.util.regex.Pattern;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;

/**
 * Used as the parameter of
 * {@link TaglibFactory#TaglibFactory(javax.servlet.ServletContext, TaglibFactoryConfiguration)}.
 * 
 * @since 2.3.22
 */
public class TaglibFactoryConfiguration {

    private ObjectWrapper objectWrapper;
    private Pattern additionalTaglibJarsPattern;

    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    /**
     * Sets the {@link ObjectWrapper} used when building the tag libraries {@link TemplateHashModel}-s from the TLD-s.
     * Usually, it should be the same {@link ObjectWrapper} that was created by
     * {@link FreemarkerServlet#createObjectWrapper} to be used inside the templates. {@code null} value is only
     * supported for backward compatibility. For custom EL functions to be exposed, it must be non-{@code null} and an
     * {@code intanceof} {@link BeansWrapper} (like a {@link DefaultObjectWrapper}).
     */
    public void setObjectWrapper(ObjectWrapper objectWrapper) {
        this.objectWrapper = objectWrapper;
    }

    public Pattern getAdditionalTaglibJarsPattern() {
        return additionalTaglibJarsPattern;
    }

    // TODO javadoc
    public void setAdditionalTaglibJarsPattern(Pattern additionalTaglibJarsPattern) {
        this.additionalTaglibJarsPattern = additionalTaglibJarsPattern;
    }

}
