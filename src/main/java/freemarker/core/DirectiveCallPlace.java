package freemarker.core;

import freemarker.ext.util.IdentityHashMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template.utility.ObjectFactory;

/**
 * Gives information about the place where a directive is called from, also lets you attach a custom data object to that
 * place. Each directive call in a template has its own {@link DirectiveCallPlace} object (even if they all call the
 * same directive with the same parameters). The life cycle of the {@link DirectiveCallPlace} object is bound to the
 * {@link Template} object that contains the directive call. Hence, the {@link DirectiveCallPlace} objects and the
 * custom data you put into them is cached together with the {@link Template}-s (and they are normally cached - see
 * {@link Configuration#getTemplate(String)}). The custom data is normally initialized on demand, that is, when
 * directive call is first attempted, via {@link #getOrCreateCustomData(Object, ObjectFactory)}.
 * 
 * <p>
 * <b>Don't implement this interface yourself</b>, as new methods can be added to it any time! It's only meant to be
 * implemented by the FreeMarker core.
 * 
 * <p>
 * This interface is currently only used for custom directive ({@link TemplateDirectiveModel},
 * {@link TemplateTransformModel}, and macro) calls.
 * 
 * @see Environment#getCurrentDirectiveCallPlace()
 * 
 * @since 2.3.22
 */
public interface DirectiveCallPlace {

    /**
     * The template that directly contains the invocation of the directive.
     */
    Template getTemplate();

    /**
     * The 1-based column number of the first character of the directive call in the template source code, or -1 if it's
     * not known.
     */
    int getBeginColumn();

    /**
     * The 1-based line number of the first character of the directive call in the template source code, or -1 if it's
     * not known.
     */
    int getBeginLine();

    /**
     * The 1-based column number of the last character of the directive call in the template source code, or -1 if it's
     * not known.
     */
    int getEndColumn();

    /**
     * The 1-based line number of the last character of the directive call in the template source code, or -1 if it's
     * not known.
     */
    int getEndLine();

    /**
     * Returns the custom data, or if that's {@code null}, then it creates and stores it in an atomic operation then
     * returns it. This method is thread-safe, however, it doesn't ensure the thread safe (like synchronized) access to
     * the custom data itself.
     * 
     * <p>
     * This method will block other calls while the {@code objectFactory} is executing, thus, the object will be
     * <em>usually</em> created only once, even if multiple threads request the value when it's still {@code null}. It
     * doesn't stand though when {@code provierIdentity} mismatches occur (see later). Furthermore, then it's also
     * possible that multiple objects created by the same {@link ObjectFactory} will be in use one the same time,
     * because of directive executions already running in parallel, and because of memory synchronization delays
     * (hardware dependent) between the threads.
     * 
     * <p>
     * Note that this feature will only work on Java 5 or later.
     * 
     * @param provierIdentity
     *            This is usually the class of the {@link TemplateDirectiveModel} that creates (and uses) the custom
     *            data, or if you are using your own class for the custom data object (as opposed to a class from some
     *            more generic API), then that class. This is needed as the same call place might calls different
     *            directives depending on runtime conditions, and so it must be ensured that these directives won't
     *            accidentally read each other's custom data, end in up with class cast exceptions or worse. In the
     *            current implementation, if there's a {@code provierIdentity} mismatch (means, the
     *            {@code proviedIdentity} object used when the custom data was last set isn't the exactly same object as
     *            the one provided with the parameter now), the previous custom data will be just ignored as if it was
     *            {@code null}. So if multiple directives that use the custom data feature use the same call place, the
     *            caching of the custom data can be inefficient, as they will keep overwriting each other's custom data.
     *            (In a more generic implementation the {@code provierIdentity} would be a key in a
     *            {@link IdentityHashMap}, but then this feature would be slower, while {@code provierIdentity}
     *            mismatches aren't occurring in most applications.)
     * @param objectFactory
     *            Called when the custom data was still {@code null} to create its initial. If can't be called because
     *            it's {@code null}, then {@code null} will be returned as the custom data. The return value of
     *            {@link ObjectFactory#createObject()} isn't allowed to be {@code null}.
     * 
     * @return The current custom data object, or possibly {@code null} if there was no {@link ObjectFactory} provided.
     * 
     * @throws CallPlaceCustomDataInitializationException
     *             If the {@link ObjectFactory} had to be invoked but failed.
     */
    Object getOrCreateCustomData(Object provierIdentity, ObjectFactory objectFactory)
            throws CallPlaceCustomDataInitializationException;

    /**
     * Tells if the nested content (body) can be safely cached, as it only depends on the template content and has no
     * side-effects (other than writing to the output). Examples of cases that give {@code false}: {@code <@foo>Name: }
     * <tt>${name}</tt>{@code</@foo>}, {@code <@foo>Name: <#if showIt>Joe</#if></@foo>}. Examples of cases that give
     * {@code true}: {@code <@foo>Name: Joe</@foo>}, {@code <@foo />}. Note that we get {@code true} for no nested
     * content, because that's equivalent with 0-length nested content in FTL.
     * 
     * <p>
     * This method returns a pessimistic result. For example, if it sees a custom directive call, it won't be able to
     * tell what it does, so it will assume that it's not cacheable.
     */
    boolean isNestedOutputCacheable();

}
