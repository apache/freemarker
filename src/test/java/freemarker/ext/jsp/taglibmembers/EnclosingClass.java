package freemarker.ext.jsp.taglibmembers;

public class EnclosingClass {

    public static class NestedClass {
        
        public static double hypotenuse(double a, double b) {
            return Math.sqrt(a * a + b * b); 
        }
        
    }
    
}
