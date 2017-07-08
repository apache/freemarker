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

import org.apache.freemarker.core.util._NullArgumentException;

public class ConverterException extends Exception {

    private final Integer row;
    private final Integer column;

    public ConverterException(String message) {
        this(message, null);
    }

    public ConverterException(String message, Throwable cause) {
        this(message, null, null, cause);
    }

    /**
     * See {@link #ConverterException(String, Integer, Integer, Throwable)}
     */
    public ConverterException(String message, Integer row, Integer column) {
        this(message, row, column, null);
    }

    /**
     * @param row The 1-based row in the source file, or {@code null}.
     * @param column The 1-based column in the source file, or {@code null}.
     */
    public ConverterException(String message, Integer row, Integer column, Throwable cause) {
        super(addLocationToMessage(message, row, column), cause);
        this.row = row;
        this.column = column;
    }

    private static String addLocationToMessage(String message, Integer row, Integer column) {
        _NullArgumentException.check("message", message);

        StringBuilder sb = new StringBuilder();

        Converter.FileConversionContext ctx = Converter.FILE_CONVERSION_CONTEXT_TLS.get();
        if (ctx != null || row != null) {
            sb.append("At ");
            if (ctx != null) {
                sb.append(ctx.getSourceFile()).append(':');
            }
            if (row != null) {
                sb.append(row).append(':');
                if (column != null) {
                    sb.append(column).append(':');
                }
            }
            sb.append(" ");
        }

        sb.append(message);
        return sb.toString();
    }

    /**
     * The 1-based row in the source file, or {@code null}.
     */
    public Integer getRow() {
        return row;
    }

    /**
     * The 1-based column in the source file, or {@code null}.
     */
    public Integer getColumn() {
        return column;
    }
}
