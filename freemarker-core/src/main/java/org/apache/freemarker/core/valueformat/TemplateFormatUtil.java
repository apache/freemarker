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
package org.apache.freemarker.core.valueformat;

import java.util.Date;

import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core._EvalUtils;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateDateModel;
import org.apache.freemarker.core.model.TemplateNumberModel;

/**
 * Utility classes for implementing {@link TemplateValueFormat}-s.
 */
public final class TemplateFormatUtil {
    
    private TemplateFormatUtil() {
        // Not meant to be instantiated
    }

    public static void checkHasNoParameters(String params) throws InvalidFormatParametersException
             {
        if (params.length() != 0) {
            throw new InvalidFormatParametersException(
                    "This number format doesn't support any parameters.");
        }
    }

    /**
     * Utility method to extract the {@link Number} from an {@link TemplateNumberModel}, and throws
     * {@link TemplateException} with a standard error message if that's {@code null}. {@link TemplateNumberModel}
     * that store {@code null} are in principle not allowed, and so are considered to be bugs in the
     * {@link ObjectWrapper} or {@link TemplateNumberModel} implementation.
     */
    public static Number getNonNullNumber(TemplateNumberModel numberModel)
            throws TemplateException, UnformattableValueException {
        Number number = numberModel.getAsNumber();
        if (number == null) {
            throw _EvalUtils.newModelHasStoredNullException(Number.class, numberModel, null);
        }
        return number;
    }

    /**
     * Utility method to extract the {@link Date} from an {@link TemplateDateModel}, and throw
     * {@link TemplateException} with a standard error message if that's {@code null}. {@link TemplateDateModel}
     * that store {@code null} are in principle not allowed, and so are considered to be bugs in the
     * {@link ObjectWrapper} or {@link TemplateNumberModel} implementation.
     */
    public static Date getNonNullDate(TemplateDateModel dateModel) throws TemplateException {
        Date date = dateModel.getAsDate();
        if (date == null) {
            throw _EvalUtils.newModelHasStoredNullException(Date.class, dateModel, null);
        }
        return date;
    }

}
