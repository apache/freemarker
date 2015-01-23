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

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template._TemplateAPI;

/**
 * Thrown when {@code ?api} is not supported by a value.
 */
class APINotSupportedTemplateException extends TemplateException {

    APINotSupportedTemplateException(Environment env, Expression blamedExpr, TemplateModel model) {
        super(null, env, blamedExpr, buildDescription(env, blamedExpr, model));
    }

    protected static _ErrorDescriptionBuilder buildDescription(Environment env, Expression blamedExpr,
            TemplateModel tm) {
        final _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(new Object[] {
                "The value doesn't support ?api. See requirements in the FreeMarker Manual. ("
                + "FTL type: ", new _DelayedFTLTypeDescription(tm),
                ", TemplateModel class: ", new _DelayedShortClassName(tm.getClass()),
                ", ObjectWapper: ", new _DelayedToString(env.getObjectWrapper()), ")"
        }).blame(blamedExpr);

        if (blamedExpr.isLiteral()) {
            desc.tip("Only adapted Java objects can possibly have API, not values created inside templates.");
        } else {
            ObjectWrapper ow = env.getObjectWrapper();
            if (ow instanceof DefaultObjectWrapper
                    && (tm instanceof SimpleHash || tm instanceof SimpleSequence)) {
                DefaultObjectWrapper dow = (DefaultObjectWrapper) ow;
                if (!dow.getUseAdaptersForContainers()) {
                    desc.tip(new Object[] {
                            "In the FreeMarker configuration, \"", Configurable.OBJECT_WRAPPER_KEY,
                            "\" is a DefaultObjectWrapper with its \"useAdaptersForContainers\" property set to "
                            + "false. Setting it to true might solves this problem." });
                    if (dow.getIncompatibleImprovements().intValue() < _TemplateAPI.VERSION_INT_2_3_22) {
                        desc.tip("Setting DefaultObjectWrapper's \"incompatibleImprovements\" to 2.3.22 or higher will "
                                + "change the default value of \"useAdaptersForContainers\" to true.");
                    }
                } else if (tm instanceof SimpleSequence && dow.getForceLegacyNonListCollections()) {
                    desc.tip(new Object[] {
                            "In the FreeMarker configuration, \"",
                            Configurable.OBJECT_WRAPPER_KEY,
                            "\" is a DefaultObjectWrapper with its \"forceLegacyNonListCollections\" property set "
                            + "to true. If you are trying to access the API of a non-List Collection, setting the "
                            + "\"forceLegacyNonListCollections\" property to false might solves this problem." });
                }
            }
        }

        return desc;
    }

}
