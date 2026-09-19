/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utilities that didn't fit elsewhere. 
 */
class MiscUtil {
    
    // Can't be instantiated
    private MiscUtil() { }

    static final String C_FALSE = "false";
    static final String C_TRUE = "true";

    /**
     * Returns the map entries in source code order of the Expression values.
     */
    static List<Map.Entry<String, Expression>> sortMapOfExpressions(Map<String, Expression> map) {
        ArrayList<Map.Entry<String, Expression>> res = new ArrayList<>(map.entrySet());
        // for sorting to source code order
        res.sort((ent1, ent2) -> {
            Expression exp1 = ent1.getValue();

            Expression exp2 = ent2.getValue();

            int res1 = exp1.beginLine - exp2.beginLine;
            if (res1 != 0) return res1;
            res1 = exp1.beginColumn - exp2.beginColumn;
            if (res1 != 0) return res1;

            if (ent1 == ent2) return 0;

            // Should never reach this
            return (ent1.getKey()).compareTo(ent1.getKey());
        });
        return res;
    }

    static Expression peelParentheses(Expression exp) {
        while (exp instanceof ParentheticalExpression) {
            exp = ((ParentheticalExpression) exp).getNestedExpression();
        }
        return exp;
    }
    
}
