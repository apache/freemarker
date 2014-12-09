package freemarker.ext.jsp;

import java.util.List;
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
public final class TaglibFactoryConfiguration {

    private ObjectWrapper objectWrapper;
    private List/*<Pattern>*/ classpathTaglibJarPatterns;
    private List/*<String>*/ classpathTlds;

    /** See {@link #setObjectWrapper(ObjectWrapper)}. */
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

    /** See {@link #setClasspathTaglibJarPatterns(List)}. */
    public List/*<Pattern>*/ getClasspathTaglibJarPatterns() {
        return classpathTaglibJarPatterns;
    }

    /**
     * Set the URL patterns of jar-s that aren't inside the web application at standard locations, yet you want
     * {@code META-INF/*.tld}-s to be discovered in them. The jar-s whose name will be matched will come from the URL
     * lists returned by the {@code URLClassLoader}-s in the class loader hierarchy. Inside those jars, we list
     * {@code META-INF/*.tld} to find the TLD-s. Note that this TLD discovery mechanism is not part of the JSP
     * specification, and is only meant to be used in development setups and in some embedded servlet setups, where you
     * just want to pick up TLD-s from the dependency jar-s, without placing them into the WAR.
     * 
     * <p>Note that {@link FreemarkerServlet} will set this value automatically if finds
     * {@code org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern} context attribute. If this is still set here,
     * the two will be concatenated, with the Jetty attributes last.
     * 
     * @param classpathTaglibJarPatterns
     *            The list of {@link Pattern}-s (not {@link String}-s) against which the URL of the jars will be
     *            matched. Maybe {@code null}. A typical example of a such pattern is {@code ".*taglib*.\.jar$"}. The
     *            pattern must match the whole jar URL, not just part of it. It's enough if one of the listed patterns
     *            match, that is, they are in logical "or" relation. The order of the patterns normally doesn't mater,
     *            however, if multiple TLD-s belong to the same taglib URI then the TLD that's inside the jar that was
     *            matched by the earlier pattern wins.
     *            
     * @see #setClasspathTlds(List)
     */
    public void setClasspathTaglibJarPatterns(List/*<Pattern>*/ classpathTaglibJarPatterns) {
        this.classpathTaglibJarPatterns = classpathTaglibJarPatterns;
    }

    /** See {@link #setClasspathTlds(List)}. */
    public List/*<String>*/ getClasspathTlds() {
        return classpathTlds;
    }

    /**
     * Sets the list of TLD resource paths for TLD-s that aren't inside the web application at standard locations, yet
     * you want them to be discovered. They will be loaded with the class loader provided by the servlet container. Note
     * that this TLD discovery mechanism is not part of the JSP specification, and is only meant to be used in
     * development setups and in some embedded servlet setups, where you just want to pick up TLD-s that are directly
     * included in your project or are in dependency jar-s, and you want that without placing them into the WAR.
     * 
     * @param classpathTlds
     *            List of {@code String}-s, maybe {@code null}. Each item is a resource path, like
     *            {@code "/META-INF/my.tld"}. Relative resource paths will be interpreted as root-relative.
     *            
     * @see #setClasspathTaglibJarPatterns(List)
     */
    public void setClasspathTlds(List/*<String>*/ classpathTlds) {
        this.classpathTlds = classpathTlds;
    }

}
