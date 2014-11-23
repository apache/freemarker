package freemarker.ext.jsp.real;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import freemarker.test.servlet.WebApplicationTestCase;

public class RealJSPTest extends WebApplicationTestCase {

    @Test
    public void test1() throws Exception {
        assertEquals("foo", getResponseContent("basic", "test1.html"));
    }

}
