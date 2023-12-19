/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.freemarker.core;

import org.apache.freemarker.core.outputformat.MarkupOutputFormat;
import org.apache.freemarker.core.outputformat.OutputFormat;

/**
 * Used as the value of the {@link ParsingConfiguration#getAutoEscapingPolicy() autoEscapingPolicy} setting.
 */
public enum AutoEscapingPolicy {

    /**
     * Don't enable auto-escaping, regardless of what the {@link OutputFormat} is. Note that a {@code
     * <#ftl auto_esc=true>} in the template will override this.
     */
    DISABLE,

    /**
     * Enable auto-escaping if the output format supports it and {@link MarkupOutputFormat#isAutoEscapedByDefault()} is
     * {@code true}.
     */
    ENABLE_IF_DEFAULT,

    /** Enable auto-escaping if the {@link OutputFormat} supports it. */
    ENABLE_IF_SUPPORTED,

    FORCE
}
