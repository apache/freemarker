package freemarker.core;

import java.util.Map;

import org.junit.Test;

public class ASTBasedErrorMessagesTest extends TemplateErrorMessageTest {
    
    @Test
    public void testOverloadSelectionError() {
        assertErrorContains("${overloads.m(null)}", "2.3.21", "overloaded");
    }
    
    @Test
    public void testInvalidRefBasic() {
        assertErrorContains("${foo}", "foo", "specify a default");
        assertErrorContains("${map[foo]}", "foo", "\\!map[", "specify a default");
    }
    
    @Test
    public void testInvalidRefDollar() {
        assertErrorContains("${$x}", "$x", "must not start with \"$\"", "specify a default");
        assertErrorContains("${map.$x}", "map.$x", "must not start with \"$\"", "specify a default");
    }

    @Test
    public void testInvalidRefAfterDot() {
        assertErrorContains("${map.foo.bar}", "map.foo", "\\!foo.bar", "after the last dot", "specify a default");
    }

    @Test
    public void testInvalidRefInSquareBrackets() {
        assertErrorContains("${map['foo']}", "map", "final [] step", "specify a default");
    }

    @Test
    public void testInvalidRefSize() {
        assertErrorContains("${map.size()}", "map.size", "?size", "specify a default");
        assertErrorContains("${map.length()}", "map.length", "?length", "specify a default");
    }

    @Override
    protected void buildDataModel(Map<String, Object> dataModel) {
        super.buildDataModel(dataModel);
        dataModel.put("overloads", new Overloads());
    }
    
    public static class Overloads {
        
        @SuppressWarnings("unused")
        public void m(String s) {}
        
        @SuppressWarnings("unused")
        public void m(int i) {}
        
    }
    
}
