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

package org.apache.freemarker.core;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelException;
import org.apache.freemarker.core.util._NullArgumentException;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */ 
public final class _CoreAPI {

    public static final int VERSION_INT_3_0_0 = Configuration.VERSION_3_0_0.intValue();

    // Can't be instantiated
    private _CoreAPI() { }

    /**
     * ATTENTION: This is used by https://github.com/kenshoo/freemarker-online. Don't break backward
     * compatibility without updating that project too! 
     */
    static public void addThreadInterruptedChecks(Template template) {
        try {
            new ThreadInterruptionSupportTemplatePostProcessor().postProcess(template);
        } catch (TemplatePostProcessorException e) {
            throw new RuntimeException("Template post-processing failed", e);
        }
    }

    /**
     * The work around the problematic cases where we should throw a {@link TemplateException}, but we are inside
     * a {@link TemplateModel} method and so we can only throw {@link TemplateModelException}-s.  
     */
    // [FM3] Get rid of this problem, then delete this method
    public static TemplateModelException ensureIsTemplateModelException(String modelOpMsg, TemplateException e) {
        if (e instanceof TemplateModelException) {
            return (TemplateModelException) e;
        } else {
            return new _TemplateModelException(
                    e.getBlamedExpression(), e.getCause(), e.getEnvironment(), modelOpMsg);
        }
    }

    public static boolean isMacro(Class<? extends TemplateModel> cl) {
        return Environment.TemplateLanguageDirective.class.isAssignableFrom(cl);
    }

    public static boolean isFunction(Class<? extends TemplateModel> cl) {
        return Environment.TemplateLanguageFunction.class.isAssignableFrom(cl);
    }

    public static boolean isTemplateLanguageCallable(Class<? extends TemplateModel> cl) {
        return Environment.TemplateLanguageCallable.class.isAssignableFrom(cl);
    }

    public static void checkVersionNotNullAndSupported(Version incompatibleImprovements) {
        _NullArgumentException.check("incompatibleImprovements", incompatibleImprovements);
        int iciV = incompatibleImprovements.intValue();
        if (iciV > Configuration.getVersion().intValue()) {
            throw new IllegalArgumentException("The FreeMarker version requested by \"incompatibleImprovements\" was "
                    + incompatibleImprovements + ", but the installed FreeMarker version is only "
                    + Configuration.getVersion() + ". You may need to upgrade FreeMarker in your project.");
        }
        if (iciV < VERSION_INT_3_0_0) {
            throw new IllegalArgumentException("\"incompatibleImprovements\" must be at least 3.0.0, but was "
                    + incompatibleImprovements);
        }
    }

}
