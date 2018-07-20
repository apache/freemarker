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

package org.apache.freemarker.spring.model.form;

import org.apache.freemarker.core.Environment;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.model.impl.SimpleNumber;

class TagIdGenerationUtils {

    private static final String ENV_VARIABLE_NAME_PREFIX = TagIdGenerationUtils.class.getName() + ".";

    private TagIdGenerationUtils() {
    }

    public static String getNextId(final Environment env, final String name) throws TemplateException {
        final String varName = ENV_VARIABLE_NAME_PREFIX + name;
        TemplateNumberModel curCountModel = (TemplateNumberModel) env.getGlobalVariable(varName);
        int curCount;

        if (curCountModel == null) {
            curCount = 1;
            curCountModel = new SimpleNumber(curCount);
        } else {
            curCount = curCountModel.getAsNumber().intValue() + 1;
            curCountModel = new SimpleNumber(curCount);
        }

        env.setGlobalVariable(varName, curCountModel);

        return name + curCount;
    }

}
