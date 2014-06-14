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

package freemarker.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Utilities that didn't fit elsewhere. 
 */
class MiscUtil {
    
    // Can't be instatiated
    private MiscUtil() { }

    static final String C_FALSE = "false";
    static final String C_TRUE = "true";
    
    /**
     * Returns the map entries in source code order of the Expression values.
     */
    static List/*Map.Entry*/ sortMapOfExpressions(Map/*<?, Expression>*/ map) {
        ArrayList res = new ArrayList(map.entrySet());
        Collections.sort(res, 
                new Comparator() {  // for sorting to source code order
                    public int compare(Object o1, Object o2) {
                        Map.Entry ent1 = (Map.Entry) o1;
                        Expression exp1 = (Expression) ent1.getValue();
                        
                        Map.Entry ent2 = (Map.Entry) o2;
                        Expression exp2 = (Expression) ent2.getValue();
                        
                        int res = exp1.beginLine - exp2.beginLine;
                        if (res != 0) return res;
                        res = exp1.beginColumn - exp2.beginColumn;
                        if (res != 0) return res;
                        
                        if (ent1 == ent2) return 0;
                        
                        // Should never reach this
                        return ((String) ent1.getKey()).compareTo((String) ent1.getKey()); 
                    }
            
        });
        return res;
    }
    
}
