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

package freemarker.ext.beans;

import freemarker.template.ObjectWrapper;

/**
 * Marker interface useful when used together with {@link MethodAppearanceFineTuner} and such customizer objects, to
 * indicate that it <b>doesn't contain reference to the {@link ObjectWrapper}</b> (so beware with non-static inner
 * classes) and can be and should be used in call introspection cache keys. This also implies that you won't
 * create many instances of the class, rather just reuse the same (or same few) instances over and over. Furthermore,
 * the instances must be thread-safe. The typical pattern in which this instance should be used is like this:
 * 
 * <pre>static class MyMethodAppearanceFineTuner implements MethodAppearanceFineTuner, SingletonCustomizer {
 *      
 *    // This is the singleton:
 *    static final MyMethodAppearanceFineTuner INSTANCE = new MyMethodAppearanceFineTuner();
 *     
 *    // Private, so it can't be constructed from outside this class.
 *    private MyMethodAppearanceFineTuner() { }
 *
 *    &#64;Override
 *    public void fineTuneMethodAppearance(...) {
 *       // Do something here, only using the parameters and maybe some other singletons. 
 *       ...
 *    }
 *     
 * }</pre>
 *
 * @since 2.3.21
 */
public interface SingletonCustomizer {

}
