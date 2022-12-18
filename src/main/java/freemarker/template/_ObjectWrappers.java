/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.template;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't.
 */
// Be careful to not refer this class in the static initializers of other classes that (indirectly) refer to this class,
// as that can lead to deadlock as the class initialization locks are acquired by the JVM! This is also why this was
// extracted from _TemplateAPI.
public final class _ObjectWrappers {
    /**
     * Kind of a dummy {@link ObjectWrapper} used at places where the internal code earlier used the
     * {@link ObjectWrapper#DEFAULT_WRAPPER} singleton, because it wasn't supposed to wrap/unwrap anything with it;
     * never use this {@link ObjectWrapper} in situations where values of arbitrary types need to be wrapped!
     * The typical situation is that we are using {@link SimpleSequence}, or {@link SimpleHash}, which always has an
     * {@link ObjectWrapper} field, even if we don't care in the given situation, and so we didn't set it explicitly.
     * The concern with the old way is that the {@link ObjectWrapper} set in the {@link Configuration} is possibly
     * more restrictive than the default, so if the template author can somehow make FreeMarker wrap something with the
     * default {@link ObjectWrapper}, then we got a security problem. So we try not to have that around, if possible.
     * The obvious fix, and the better engineering would be just use a {@link TemplateSequenceModel} or
     * {@link TemplateHashModelEx2} implementation at those places, which doesn't have an {@link ObjectWrapper} (and
     * doesn't have the overhead of said implementations either). But, some user code might casts the values it
     * receives (as directive argument for example) to {@link SimpleSequence} or {@link SimpleHash}, instead of to
     * {@link TemplateSequenceModel} or {@link TemplateHashModelEx2}. Such user code is wrong, but still, if it worked
     * so far fine (especially as sequence/hash literals are implemented by these "Simple" classes), it's better if it
     * keeps working when they upgrade to 2.3.30. Such user code will be still out of luck if it also tries to add items
     * which are not handled by {@link SimpleObjectWrapper}, but such abuse is even more unlikely, and this is how far
     * we could go with this backward compatibility hack.
     *
     * @since 2.3.30
     */
    public static final SimpleObjectWrapper SAFE_OBJECT_WRAPPER;
    static {
        SAFE_OBJECT_WRAPPER = new SimpleObjectWrapper(Configuration.VERSION_2_3_0);
        SAFE_OBJECT_WRAPPER.writeProtect();
    }

}
