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

package org.freemarker.converter.test;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.commons.io.FileUtils.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.freemarker.converter.Converter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class ConverterTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected File srcDir;
    protected File dstDir;

    @Before
    public void setup() throws IOException {
        srcDir = folder.newFolder("src");
        dstDir = folder.newFolder("dst");
        createSourceFiles();
    }

    protected abstract void createSourceFiles() throws IOException;

    protected String readConversionMarkersFile() throws IOException {
        return readConversionMarkersFile(false);
    }

    protected String readConversionMarkersFile(boolean delete) throws IOException {
        File markersFile = getConversionMarkersFile();
        assertTrue(markersFile.isFile());
        String content = readFileToString(markersFile, UTF_8);
        if (!markersFile.delete()) {
            throw new IOException("Failed to delete file: " + markersFile);
        }
        return content;
    }

    protected File getConversionMarkersFile() {
        return new File(dstDir, Converter.CONVERSION_MARKERS_FILE_NAME);
    }

}
