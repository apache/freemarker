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

import freemarker.template.SimpleNumber;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateNumberModel;

/**
 * A simple implementation of the <tt>TemplateNumberModel</tt>
 * interface. Note that this class is immutable.
 */
final class NumberLiteral extends Expression implements TemplateNumberModel {

    private final Number value;

    public NumberLiteral(Number value) {
        this.value = value;
    }
    
    @Override
    TemplateModel _eval(Environment env) {
        return new SimpleNumber(value);
    }

    @Override
    public String evalAndCoerceToPlainText(Environment env) throws TemplateException {
        return env.formatNumberToPlainText(this, this, false);
    }

    @Override
    public Number getAsNumber() {
        return value;
    }
    
    String getName() {
        return "the number: '" + value + "'";
    }

    @Override
    public String getCanonicalForm() {
        return value.toString();
    }
    
    @Override
    String getNodeTypeSymbol() {
        return getCanonicalForm();
    }
    
    @Override
    boolean isLiteral() {
        return true;
    }

    @Override
    protected Expression deepCloneWithIdentifierReplaced_inner(
            String replacedIdentifier, Expression replacement, ReplacemenetState replacementState) {
        return new NumberLiteral(value);
    }
    
    @Override
    int getParameterCount() {
        return 0;
    }

    @Override
    Object getParameterValue(int idx) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    ParameterRole getParameterRole(int idx) {
        throw new IndexOutOfBoundsException();
    }
    
}
