/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import freemarker.ext.util.IdentityHashMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateTransformModel;
import freemarker.template.utility.ObjectFactory;

/**
 * Gives information about the place where a directive is called from, also lets you attach a custom data object to that
 * place. Each directive call in a template has its own {@link DirectiveCallPlace} object (even when they call the same
 * directive with the same parameters). The life cycle of the {@link DirectiveCallPlace} object is bound to the
 * {@link Template} object that contains the directive call. Hence, the {@link DirectiveCallPlace} object and the custom
 * data you put into it is cached together with the {@link Template} (and templates are normally cached - see
 * {@link Configuration#getTemplate(String)}). The custom data is normally initialized on demand, that is, when the
 * directive call is first executed, via {@link #getOrCreateCustomData(Object, ObjectFactory)}.
 * 
 * <p>
 * Currently this method doesn't give you access to the {@link Template} object, because it's probable that future
 * versions of FreeMarker will be able to use the same parsed representation of a "file" for multiple {@link Template}
 * objects. Then the call place will be bound to the parsed representation, not to the {@link Template} objects that are
 * based on it.
 * 
 * <p>
 * <b>Don't implement this interface yourself</b>, as new methods can be added to it any time! It's only meant to be
 * implemented by the FreeMarker core.
 * 
 * <p>
 * This interface is currently only used for custom directive calls (that is, a {@code <@...>} that calls a
 * {@link TemplateDirectiveModel}, {@link TemplateTransformModel}, or a macro).
 * 
 * @see Environment#getCurrentDirectiveCallPlace()
 * 
 * @since 2.3.22
 */
public interface DirectiveCallPlace {

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
     * not known. If the directive has an end-tag ({@code </@...>}), then it points to the last character of that.
     */
    int getEndColumn();

    /**
     * The 1-based line number of the last character of the directive call in the template source code, or -1 if it's
     * not known. If the directive has an end-tag ({@code </@...>}), then it points to the last character of that.
     */
    int getEndLine();

    /**
     * Returns the custom data, or if that's {@code null}, then it creates and stores it in an atomic operation then
     * returns it. This method is thread-safe, however, it doesn't ensure thread safe (like synchronized) access to the
     * custom data itself. See the top-level documentation of {@link DirectiveCallPlace} to understand the scope and
     * life-cycle of the custom data. Be sure that the custom data only depends on things that get their final value
     * during template parsing, not on runtime settings.
     * 
     * <p>
     * This method will block other calls while the {@code objectFactory} is executing, thus, the object will be
     * <em>usually</em> created only once, even if multiple threads request the value when it's still {@code null}. It
     * doesn't stand though when {@code providerIdentity} mismatches occur (see later). Furthermore, then it's also
     * possible that multiple objects created by the same {@link ObjectFactory} will be in use on the same time, because
     * of directive executions already running in parallel, and because of memory synchronization delays (hardware
     * dependent) between the threads.
     * 
     * <p>
     * Note that this feature will only work on Java 5 or later.
     * 
     * @param providerIdentity
     *            This is usually the class of the {@link TemplateDirectiveModel} that creates (and uses) the custom
     *            data, or if you are using your own class for the custom data object (as opposed to a class from some
     *            more generic API), then that class. This is needed as the same call place might calls different
     *            directives depending on runtime conditions, and so it must be ensured that these directives won't
     *            accidentally read each other's custom data, ending up with class cast exceptions or worse. In the
     *            current implementation, if there's a {@code providerIdentity} mismatch (means, the
     *            {@code providerIdentity} object used when the custom data was last set isn't the exactly same object
     *            as the one provided with the parameter now), the previous custom data will be just ignored as if it
     *            was {@code null}. So if multiple directives that use the custom data feature use the same call place,
     *            the caching of the custom data can be inefficient, as they will keep overwriting each other's custom
     *            data. (In a more generic implementation the {@code providerIdentity} would be a key in a
     *            {@link IdentityHashMap}, but then this feature would be slower, while {@code providerIdentity}
     *            mismatches aren't occurring in most applications.)
     * @param objectFactory
     *            Called when the custom data wasn't yet set, to create its initial value. If this parameter is
     *            {@code null} and the custom data wasn't set yet, then {@code null} will be returned. The returned
     *            value of {@link ObjectFactory#createObject()} can be any kind of object, but can't be {@code null}.
     * 
     * @return The current custom data object, or possibly {@code null} if there was no {@link ObjectFactory} provided.
     * 
     * @throws CallPlaceCustomDataInitializationException
     *             If the {@link ObjectFactory} had to be invoked but failed.
     */
    Object getOrCreateCustomData(Object providerIdentity, ObjectFactory objectFactory)
            throws CallPlaceCustomDataInitializationException;

    /**
     * Tells if the nested content (the body) can be safely cached, as it only depends on the template content (not on
     * variable values and such) and has no side-effects (other than writing to the output). Examples of cases that give
     * {@code false}: {@code <@foo>Name: } <tt>${name}</tt>{@code</@foo>},
     * {@code <@foo>Name: <#if showIt>Joe</#if></@foo>}. Examples of cases that give {@code true}:
     * {@code <@foo>Name: Joe</@foo>}, {@code <@foo />}. Note that we get {@code true} for no nested content, because
     * that's equivalent with 0-length nested content in FTL.
     * 
     * <p>
     * This method returns a pessimistic result. For example, if it sees a custom directive call, it can't know what it
     * does, so it will assume that it's not cacheable.
     */
    boolean isNestedOutputCacheable();

}
