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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

class JavaTemplateNumberFormat extends TemplateNumberFormat {
    
    private final String pattern;
    private final DecimalFormat decimalFormat;
    
    public JavaTemplateNumberFormat(String pattern, Locale locale) throws InvalidFormatDescriptorException {
        this.pattern = pattern;
        try {
            decimalFormat = new DecimalFormat(pattern, new DecimalFormatSymbols(locale));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            throw new InvalidFormatDescriptorException(msg != null ? msg : "Invalid DecimalFormat pattern", pattern, e);
        }
    }

    @Override
    public String format(TemplateNumberModel numberModel) throws UnformattableNumberException, TemplateModelException {
        return decimalFormat.format(numberModel.getAsNumber());
    }

    @Override
    public <MO extends TemplateMarkupOutputModel> MO format(TemplateNumberModel dateModel,
            MarkupOutputFormat<MO> outputFormat) throws UnformattableNumberException, TemplateModelException {
        return null;
    }

    @Override
    public String getDescription() {
        return pattern;
    }

    @Override
    public boolean isLocaleBound() {
        return true;
    }

}
