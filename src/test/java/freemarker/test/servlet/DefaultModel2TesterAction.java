package freemarker.test.servlet;

import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;

/**
 * Used by {@link Model2TesterServlet} if no action was explicitly specified.
 */
public class DefaultModel2TesterAction implements Model2Action {

    public static final DefaultModel2TesterAction INSTANCE = new DefaultModel2TesterAction();
    
    protected DefaultModel2TesterAction() { }

    @SuppressWarnings("boxing")
    public String execute(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.setAttribute("s", "abc");
        req.setAttribute("n", 123);
        req.setAttribute("t", true);
        req.setAttribute("f", false);
        req.setAttribute("ls", Arrays.asList("a", "b", "c"));
        req.setAttribute("m", ImmutableMap.<String, Integer>builder().put("a", 11).put("b", 22).put("c", 33).build());
        req.setAttribute("b", new TestBean("Joe", 30, true));
        req.setAttribute("d", new Date(123456L));
        req.setAttribute("lsob", Arrays.asList(
                new TestBean("Joe", 30, true), new TestBean("Fred", 25, false), new TestBean("Emma", 28, true)));
        return null;
    }
    
    public static class TestBean {
        private String name;
        private int age;
        private boolean maried;
        
        public TestBean(String name, int age, boolean maried) {
            this.name = name;
            this.age = age;
            this.maried = maried;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isMaried() {
            return maried;
        }

        public void setMaried(boolean maried) {
            this.maried = maried;
        }
        
    }

}
