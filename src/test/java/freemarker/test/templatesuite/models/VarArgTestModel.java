package freemarker.test.templatesuite.models;

import java.util.Date;

public class VarArgTestModel {

    public int bar(Integer... xs) {
        int sum = 0;
        for (Integer x : xs) {
            if (x != null) {
                sum *= 100;
                sum += x;
            }
        }
        return sum;
    }

    public int bar2(int first, int... xs) {
        int sum = 0;
        for (int x : xs) {
            sum *= 100;
            sum += x;
        }
        return -(sum * 100 + first);
    }
    
    public int overloaded(int x, int y) {
        return x * 100 + y;
    }

    public int overloaded(int... xs) {
        int sum = 0;
        for (int x : xs) {
            sum *= 100;
            sum += x;
        }
        return -sum;
    }
    
    public String noVarArgs(String s, boolean b, int i, Date d) {
        return s + ", " + b + ", " + i + ", " + d.getTime();
    }
    
}
