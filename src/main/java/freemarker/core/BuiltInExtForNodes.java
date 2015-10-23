package freemarker.core;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNodeModelExt;

/**
 * Created by Pmuruge on 10/23/2015.
 */
public class BuiltInExtForNodes {

    static class previousSiblingBI extends BuiltInExtForNode {
        @Override
        TemplateModel calculateResult(TemplateNodeModelExt nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getPreviousSibling();
        }
    }

    static class nextSiblingBI extends  BuiltInExtForNode {
        @Override
        TemplateModel calculateResult(TemplateNodeModelExt nodeModel, Environment env) throws TemplateModelException {
            return nodeModel.getNextSibling();
        }
    }

}
