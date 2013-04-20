package freemarker.ext.beans;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class PrallelObjectIntrospectionTest extends AbstractParallelIntrospectionTest {

    public PrallelObjectIntrospectionTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        new PrallelObjectIntrospectionTest("almostForever")
                .testReliability(Integer.MAX_VALUE);
    }
    
    protected TemplateHashModel getWrappedEntity(int objIdx)
    throws TemplateModelException {
        return (TemplateHashModel) getBeansWrapper().wrap(
                ManyObjectsOfDifferentClasses.OBJECTS[objIdx]);
    }
    
}
