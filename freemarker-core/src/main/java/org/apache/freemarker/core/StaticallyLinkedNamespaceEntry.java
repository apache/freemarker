package org.apache.freemarker.core;

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

import org.apache.freemarker.core.model.TemplateCallableModel;
import org.apache.freemarker.core.model.TemplateDirectiveModel;
import org.apache.freemarker.core.model.TemplateFunctionModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.CommonSupplier;
import org.apache.freemarker.core.util._NullArgumentException;

/**
 * A template language namespace entry that's linked statically to the referring template (TODO [FM3] for planned
 * Dialects feature, currently unused).
 * Static linking allows detecting mistakes during template parsing (as opposed to during the later template
 * processing), can make accessing the value faster (by avoiding runtime lookup), and allows callables to have
 * parse-time effects. Currently used in {@link Dialect}. 
 */
//TODO [FM3][FREEMARKER-99][DIALECTS] will be public. Also move it into core.dialect?
final class StaticallyLinkedNamespaceEntry {
    
    private final String name;
    private final TemplateModel value;
    private final CommonSupplier<ASTDirective> directiveCallNodeFactory;
    private final CommonSupplier<ASTExpFunctionCall> functionCallNodeFactory;
    
    /**
     * @param name
     *            Variable name in the namespace of the dialect
     * @param value
     *            The runtime value of the entry. Not {@code null}.
     *            If this entry is a {@link TemplateCallableModel} that's not callable dynamically (as it has
     *            parse-time effects or such dependencies), then the corresponding {@code execute} method should to
     *            throw exception explaining that, but the {@link TemplateCallableModel} will be still used by the
     *            parser to retrieve meta-data such as
     *            {@link TemplateDirectiveModel#getDirectiveArgumentArrayLayout()},
     *            {@link TemplateDirectiveModel#isNestedContentSupported()},
     *            {@link TemplateFunctionModel#getFunctionArgumentArrayLayout()}.
     * @param directiveCallNodeFactory
     *            If this entry is usable as directive, the factory for the corresponding {@linkplain ASTNode AST node}.
     *            At least one of {@code directiveCallNodeFactory} and {@code functionCallNodeFactory} must be
     *            non-{@code null}. It must be ensured that the behavior of this node is consistent with argument array
     *            layout and other meta-data present in the entry {@code value}.
     * @param functionCallNodeFactory
     *            If this entry is usable as function (or like an FM2 "built-in"), the factory for the corresponding
     *            {@linkplain ASTNode AST node}. Not {@code null}. It must be ensured that the behavior of this node is consistent with argument array
     *            layout and other meta-data present in the entry {@code value}.
     */
    public StaticallyLinkedNamespaceEntry(
            String name, TemplateModel value,
            CommonSupplier<ASTDirective> directiveCallNodeFactory,
            CommonSupplier<ASTExpFunctionCall> functionCallNodeFactory) {
        _NullArgumentException.check("name", name);
        this.name = name;
        if (directiveCallNodeFactory == null && functionCallNodeFactory == null) {
            throw new IllegalArgumentException(
                    "At least one of \"directiveCallNodeFactory\" and \"functionCallNodeFactory\" must be non-null.");
        }
        this.directiveCallNodeFactory = directiveCallNodeFactory;
        this.functionCallNodeFactory = functionCallNodeFactory;
        this.value = value;
    }

    /**
     * See similarly named
     * {@linkplain #StaticallyLinkedNamespaceEntry(String, TemplateModel, CommonSupplier, CommonSupplier) constructor}
     * parameter.
     */
    public String getName() {
        return name;
    }

    /**
     * See similarly named
     * {@linkplain #StaticallyLinkedNamespaceEntry(String, TemplateModel, CommonSupplier, CommonSupplier) constructor}
     * parameter.
     */
    public CommonSupplier<ASTDirective> getDirectiveCallNodeFactory() {
        return directiveCallNodeFactory;
    }

    /**
     * See similarly named
     * {@linkplain #StaticallyLinkedNamespaceEntry(String, TemplateModel, CommonSupplier, CommonSupplier) constructor}
     * parameter.
     */
    public CommonSupplier<ASTExpFunctionCall> getFunctionCallNodeFactory() {
        return functionCallNodeFactory;
    }

    /**
     * See similarly named
     * {@linkplain #StaticallyLinkedNamespaceEntry(String, TemplateModel, CommonSupplier, CommonSupplier) constructor}
     * parameter.
     */
    public TemplateModel getValue() {
        return value;
    }

}
