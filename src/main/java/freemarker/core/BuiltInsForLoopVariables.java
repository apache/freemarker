package freemarker.core;

import freemarker.core.IteratorBlock.IterationContext;
import freemarker.template.SimpleNumber;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;


class BuiltInsForLoopVariables {
    
    static class indexBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new SimpleNumber(iterCtx.getIndex());
        }
        
    }
    
    static class counterBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return new SimpleNumber(iterCtx.getIndex() + 1);
        }
        
    }

    static class has_nextBI extends BuiltInForLoopVariable {

        TemplateModel calculateResult(IterationContext iterCtx, Environment env) throws TemplateException {
            return iterCtx.hasNext() ? TemplateBooleanModel.TRUE : TemplateBooleanModel.FALSE;
        }
        
    }
    
}
