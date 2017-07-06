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

package org.apache.freemarker.converter;

import java.io.File;

public interface ConversionWarnReceiver {

    /**
     * Set the file to which the subsequent {@link #warn} calls will refer to.
     * @param sourceFile
     */
    void setSourceFile(File sourceFile);

    /**
     * @param row
     *         1-based column in the source file
     * @param col
     *         1-based row in the source file
     * @param message
     *         Not {@code null}
     *
     * @throws IllegalStateException
     *         If no file was set with {@link #setSourceFile(File)}
     */
    void warn(int row, int col, String message);
}
