package freemarker.ext.beans;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class PrallelStaticIntrospectionTest extends AbstractParallelIntrospectionTest {

    private static final String STATICS_CLASS_CONTAINER_CLASS_NAME
            = ManyStaticsOfDifferentClasses.class.getName();

    public PrallelStaticIntrospectionTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        new PrallelStaticIntrospectionTest("almostForever")
                .testReliability(Integer.MAX_VALUE);
    }
    
    protected TemplateHashModel getWrappedEntity(int clIdx)
    throws TemplateModelException {
        return (TemplateHashModel) getBeansWrapper().getStaticModels().get(
                STATICS_CLASS_CONTAINER_CLASS_NAME + "$C"
                + clIdx);
    }
    
}
