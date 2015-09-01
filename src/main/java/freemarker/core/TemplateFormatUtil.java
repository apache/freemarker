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

import java.util.Date;

import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

/**
 * @since 2.3.24 
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
     * Utility method to extract the {@link Number} from an {@link TemplateNumberModel}, and throw
     * {@link UnformattableNumberException} with a standard error message if that's {@code null}.
     */
    public static Number getNonNullNumber(TemplateNumberModel numberModel)
            throws TemplateModelException, UnformattableNumberException {
        Number number = numberModel.getAsNumber();
        if (number == null) {
            throw EvalUtil.newModelHasStoredNullException(Number.class, numberModel, null);
        }
        return number;
    }

    /**
     * Utility method to extract the {@link Date} from an {@link TemplateDateModel}, and throw
     * {@link UnformattableDateException} with a standard error message if that's {@code null}.
     */
    public static Date getNonNullDate(TemplateDateModel dateModel) throws TemplateModelException {
        Date date = dateModel.getAsDate();
        if (date == null) {
            throw EvalUtil.newModelHasStoredNullException(Date.class, dateModel, null);
        }
        return date;
    }

}
