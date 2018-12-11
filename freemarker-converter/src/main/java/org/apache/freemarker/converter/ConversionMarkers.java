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

import org.apache.freemarker.core.util._NullArgumentException;

/**
 * Stores markers that apply to a given position in the source or destination file.
 */
public class ConversionMarkers {

    private final List<Entry> sourceMarkers = new ArrayList<>();
    private final List<Entry> destinationMarkers = new ArrayList<>();

    /**
     * Adds a marker to the source file.
     * @param row
     *         1-based column in the source file
     * @param col
     *         1-based row in the source file
     * @param message
*         Not {@code null}
     */
    public void markInSource(int row, int col, Type type, String message) {
        sourceMarkers.add(new Entry(row, col, Type.WARN, message));
    }

    /**
     * Adds a marker to the destination file.
     *
     * @param row
     *         1-based column in the source file
     * @param col
     *         1-based row in the source file
     * @param message
     *         Not {@code null}
     */
    public void markInDestination(int row, int col, Type type, String message) {
        destinationMarkers.add(new Entry(row, col, type, message));
    }

    public enum Type {
        WARN, TIP
    }

    public class Entry {
        private final int row;
        private final int column;
        private final Type type;
        private final String message;

        public Entry(int row, int column, Type type, String message) {
            _NullArgumentException.check("type", type);
            _NullArgumentException.check("message", message);

            this.row = row;
            this.column = column;
            this.type = type;
            this.message = message;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }

        public Type getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    public List<Entry> getSourceMarkers() {
        return sourceMarkers;
    }

    public List<Entry> getDestinationMarkers() {
        return destinationMarkers;
    }

}
