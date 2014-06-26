package freemarker.core;

import org.junit.Test;

public class TypeErrorMessagesTest extends TemplateErrorMessageTest {
    
    @Test
    public void testNumericalBinaryOperator() {
        assertErrorContains("${n - s}", "\"-\"", "right-hand", "number", "string");
        assertErrorContains("${s - n}", "\"-\"", "left-hand", "number", "string");
    }
    
    @Test
    public void testGetterMistake() {
        assertErrorContains("${bean.getX}", "${...}",
                "number", "string", "method", "obj.getSomething", "obj.something");
        assertErrorContains("${1 * bean.getX}", "right-hand",
                "number", "\\!string", "method", "obj.getSomething", "obj.something");
        assertErrorContains("<#if bean.isB></#if>", "condition",
                "boolean", "method", "obj.isSomething", "obj.something");
        assertErrorContains("<#if bean.isB></#if>", "condition",
                "boolean", "method", "obj.isSomething", "obj.something");
        assertErrorContains("${bean.voidM}",
                "string", "method", "\\!()");
        assertErrorContains("${bean.intM}",
                "string", "method", "obj.something()");
        assertErrorContains("${bean.intMP}",
                "string", "method", "obj.something(params)");
    }
    
}
