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

import java.util.ArrayList;
import java.util.List;

/**
 * Receives source code markings with positions.
 */
public class ConversionMarkerReceiver {

    private final List<Entry> sourceFileMarkers = new ArrayList<>();
    private final List<Entry> destinationFileMarkers = new ArrayList<>();

    /**
     * @param row
     *         1-based column in the source file
     * @param col
     *         1-based row in the source file
     * @param message
     *         Not {@code null}
     */
    public void warnInSource(int row, int col, String message);

    /**
     * Similar to {@link #warnInSource(int, int, String)}, but adds a tipInOutput instead of a warning message.
     */
    public void tipInOutput(int row, int col, String message);

    public enum Type {
        WARN, TIP
    }

    public class Entry {
        private final int row;
        private final int column;
        private final String message;

        public Entry(int row, int column, String message) {
            this.row = row;
            this.column = column;
            this.message = message;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public String getMessage() {
            return message;
        }
    }

}
