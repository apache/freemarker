package freemarker.test.utility;

import java.io.IOException;
import java.util.Map;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.NullWriter;

public class NoOutputDirective implements TemplateDirectiveModel {

    public static final NoOutputDirective INSTANCE = new NoOutputDirective(); 
    
    private NoOutputDirective() {
        //
    }

    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        if (!params.isEmpty()) {
            throw new TemplateModelException("This directivey doesn't support any parameters.");
        }
        body.render(NullWriter.INSTANCE);
    }

}
