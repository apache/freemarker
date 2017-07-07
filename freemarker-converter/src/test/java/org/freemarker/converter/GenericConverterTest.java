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

package org.freemarker.converter;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.commons.io.FileUtils.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.freemarker.converter.ConversionMarkers;
import org.apache.freemarker.converter.MissingRequiredPropertyException;
import org.apache.freemarker.converter.PropertyValidationException;
import org.apache.freemarker.converter.Converter;
import org.apache.freemarker.converter.ConverterException;
import org.freemarker.converter.test.ConverterTest;
import org.junit.Test;

/**
 * Test the common functionality implemented in {@link Converter}.
 */
public class GenericConverterTest extends ConverterTest {

    protected void createSourceFiles() throws IOException {
        write(new File(srcDir, "t1.txt"), "t1", UTF_8);
        write(new File(srcDir, "t2.txt"), "t2", UTF_8);

        File srcSubDir = new File(srcDir, "sub");
        if (!srcSubDir.mkdir()) {
            throw new IOException("Failed to create directory: " + srcSubDir);
        }
        write(new File(srcSubDir, "st1.txt"), "st1", UTF_8);
        write(new File(srcSubDir, "st2.txt"), "st2", UTF_8);

        File srcSub2Lv2Dir = new File(new File(srcDir, "sub2"), "lv2");
        if (!srcSub2Lv2Dir.mkdirs()) {
            throw new IOException("Failed to create directory: " + srcSubDir);
        }
        write(new File(srcSub2Lv2Dir, "s2lv2t1.txt"), "s2lv2t1", UTF_8);
    }

    @Test
    public void testWithSourceFile() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(new File(srcDir, "t1.txt"));
        converter.setDestinationDirectory(dstDir);
        converter.execute();

        assertEquals("T1", readFileToString(new File(dstDir, "t1.txt.uc"), UTF_8));
        assertFalse(new File(dstDir, "t2.txt.uc").exists());
        assertFalse(new File(dstDir, "sub").exists());
    }

    @Test
    public void testWithSourceDirectory() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        converter.execute();

        assertEquals("T1", readFileToString(new File(dstDir, "t1.txt.uc"), UTF_8));
        assertEquals("T2", readFileToString(new File(dstDir, "t2.txt.uc"), UTF_8));
        assertEquals("ST1", readFileToString(new File(new File(dstDir, "sub"), "st1.txt.uc"), UTF_8));
        assertEquals("ST2", readFileToString(new File(new File(dstDir, "sub"), "st2.txt.uc"), UTF_8));
        assertEquals("S2LV2T1",
                readFileToString(new File(new File(new File(dstDir, "sub2"), "lv2"), "s2lv2t1.txt.uc"),
                        UTF_8));
    }

    @Test
    public void testCanBeExecutedOnlyOnce() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        converter.execute();
        try {
            converter.execute();
            fail();
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void testSourcePropertyInvalid() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(new File(srcDir, "noSuchFile"));
        converter.setDestinationDirectory(dstDir);
        try {
            converter.execute();
            fail();
        } catch (PropertyValidationException e) {
            assertThat(e.getMessage(), containsString("noSuchFile"));
        }
    }

    @Test
    public void testCreateDstDisabled() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(srcDir);
        File dstDir = new File(new File(this.dstDir, "foo"), "bar");
        converter.setDestinationDirectory(dstDir);
        try {
            converter.execute();
            fail();
        } catch (PropertyValidationException e) {
            assertThat(e.getMessage(), containsString("foo"));
            assertFalse(dstDir.exists());
        }
    }

    @Test
    public void testCreateDstEnabled() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(srcDir);
        File dstDir = new File(new File(this.dstDir, "foo"), "bar");
        converter.setDestinationDirectory(dstDir);
        converter.setCreateDestinationDirectory(true);
        converter.execute();
        assertTrue(dstDir.exists());
    }

    @Test
    public void testSourcePropertyRequired() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setDestinationDirectory(dstDir);
        try {
            converter.execute();
            fail();
        } catch (MissingRequiredPropertyException e) {
            assertEquals(ToUpperCaseConverter.PROPERTY_NAME_SOURCE, e.getPropertyName());
        }
    }

    @Test
    public void testDestinationDirPropertyRequired() throws ConverterException, IOException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(srcDir);
        try {
            converter.execute();
            fail();
        } catch (MissingRequiredPropertyException e) {
            assertEquals(ToUpperCaseConverter.PROPERTY_NAME_DESTINATION_DIRECTORY, e.getPropertyName());
        }
    }

    @Test
    public void testMarksStored() throws IOException, ConverterException {
        write(new File(srcDir, "warn.txt"), "[trigger warn]", UTF_8);
        write(new File(srcDir, "tip.txt"), "[trigger tip]", UTF_8);

        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        converter.execute();

        String markersFileContent = readConversionMarkersFile();
        assertThat(markersFileContent, allOf(
                containsString("[WARN]"),
                containsString("warn.txt:1:2"),
                containsString("Warn message")));
        assertThat(markersFileContent, allOf(
                containsString("[TIP]"),
                containsString("tip.txt.uc:1:2"),
                containsString("Tip message")));
    }

    @Test
    public void emptyMarkFileCreated() throws IOException, ConverterException {
        ToUpperCaseConverter converter = new ToUpperCaseConverter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        converter.execute();

        File markersFile = new File(dstDir, Converter.CONVERSION_MARKERS_FILE_NAME);
        assertTrue(markersFile.exists());
    }

    public static class ToUpperCaseConverter extends Converter {

        @Override
        protected void convertFile(FileConversionContext ctx) throws ConverterException, IOException {
            String content = IOUtils.toString(ctx.getSourceStream(), StandardCharsets.UTF_8);
            ctx.setDestinationFileName(ctx.getSourceFileName() + ".uc");
            if (content.contains("[trigger warn]")) {
                ctx.getConversionMarkers().markInSource(
                        1, 2, ConversionMarkers.Type.WARN, "Warn message");
            }
            if (content.contains("[trigger tip]")) {
                ctx.getConversionMarkers().markInDestination(
                        1, 2, ConversionMarkers.Type.TIP, "Tip message");
            }
            IOUtils.write(content.toUpperCase(), ctx.getDestinationStream(), StandardCharsets.UTF_8);
        }

    }

}
