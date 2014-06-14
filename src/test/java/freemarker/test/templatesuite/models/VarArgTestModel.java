/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
