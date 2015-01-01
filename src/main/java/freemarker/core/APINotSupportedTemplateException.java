package freemarker.core;

import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.SimpleSequence;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;
import freemarker.template._TemplateAPI;

class APINotSupportedTemplateException extends TemplateException {

    APINotSupportedTemplateException(Environment env, Expression blamedExpr, TemplateModel model) {
        super(null, env, blamedExpr, buildDescription(env, blamedExpr, model));
    }

    protected static _ErrorDescriptionBuilder buildDescription(Environment env, Expression blamedExpr,
            TemplateModel model) {
        final _ErrorDescriptionBuilder desc = new _ErrorDescriptionBuilder(new Object[] {
                "The value doesn't support ?api. See preconditions in the FreeMarker Manual. (The model class of the "
                + "value is: ", new _DelayedShortClassName(model.getClass()), ".) "
        }).blame(blamedExpr);
        
        if (blamedExpr.isLiteral()) {
            desc.tip("Only adapted Java objects can possibly have API, not values created inside templates.");
        } else {
            ObjectWrapper ow = env.getObjectWrapper();
            if (ow instanceof DefaultObjectWrapper
                    && (model instanceof SimpleHash || model instanceof SimpleSequence)) {
                DefaultObjectWrapper dow = (DefaultObjectWrapper) ow;
                if (!dow.getUseAdaptersForContainers()) {
                    desc.tip(new Object[] {
                            "In the FreeMarker configuration the \"",
                            Configurable.OBJECT_WRAPPER_KEY, "\" is ", new _DelayedToString(dow),
                            ". Only adapters have ?api functionality, so you have to set its "
                                    + "\"useAdaptersForContainers\" property to true." });
                    if (dow.getIncompatibleImprovements().intValue() < _TemplateAPI.VERSION_INT_2_3_22) {
                        desc.tip(new Object[] {
                                "Setting DefaultObjectWrapper's \"incompatibleImprovements\" to 2.3.22 will change the "
                                        + "default value of \"useAdaptersForContainers\" to true." });
                    }
                } else if (model instanceof SimpleSequence) {
                    desc.tip(new Object[] {
                            "In the FreeMarker configuration the \"",
                            Configurable.OBJECT_WRAPPER_KEY, "\" is ", new _DelayedToString(dow),
                            ". If you are trying to access the API of a non-List Collection, setting the "
                            + "\"forceLegacyNonListCollections\" property to false should solve this problem." });
                }
            }
            
            if (!(model instanceof TemplateHashModel || model instanceof TemplateSequenceModel)) {
                desc.tip(new Object[] { "It depends on the \"",
                        Configurable.OBJECT_WRAPPER_KEY,
                        "\", but usually only adapted Map-s, List-s and other Collection-s used to have ?api support. "
                        + "The value is a(n) ", new _DelayedFTLTypeDescription(model),
                        ", so probably it doesn't adapt neither." });
            }
        }

        return desc;
    }

}
