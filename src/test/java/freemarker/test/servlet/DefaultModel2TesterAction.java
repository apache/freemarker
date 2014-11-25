package freemarker.test.servlet;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;

/**
 * Used by {@link Model2TesterServlet} if no action was explicitly specified.
 */
public final class DefaultModel2TesterAction implements Model2Action {

    public static final DefaultModel2TesterAction INSTANCE = new DefaultModel2TesterAction();
    
    private DefaultModel2TesterAction() {
        // Not meant to be instantiated
    }

    @SuppressWarnings("boxing")
    public String execute(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.setAttribute("s", "abc");
        req.setAttribute("n", 123);
        req.setAttribute("t", true);
        req.setAttribute("f", false);
        req.setAttribute("ls", Arrays.asList("a", "b", "c"));
        req.setAttribute("m", ImmutableMap.<String, Integer>builder().put("a", 11).put("b", 22).put("c", 33).build());
        req.setAttribute("b", new TestBean(1, 2));
        return null;
    }
    
    public static class TestBean {
        private int x;
        private int y;
        
        public TestBean(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }
        
        public void setX(int x) {
            this.x = x;
        }
        
        public int getY() {
            return y;
        }
        
        public void setY(int y) {
            this.y = y;
        }
        
        public int sum(int z) {
            return x + y + z;
        }
        
    }

}
