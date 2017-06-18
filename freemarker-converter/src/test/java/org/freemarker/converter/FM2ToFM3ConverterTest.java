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
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.freemarker.converter.FM2ToFM3Converter;
import org.apache.freemarker.converter.ConverterException;
import org.freemarker.converter.test.ConverterTest;
import org.junit.Test;

import freemarker.template.Configuration;

public class FM2ToFM3ConverterTest extends ConverterTest {

    protected void createSourceFiles() throws IOException {
        //
    }

    @Test
    public void testMixed() throws IOException, ConverterException {
        assertConvertedSame("s1\n  <#if t>\n    ${var}\n  </#if>\ns2");
    }

    @Test
    public void testInterpolations() throws IOException, ConverterException {
        assertConvertedSame("${var}");
        assertConvertedSame("${  var\n}");
    }

    @Test
    public void testExpressions() throws IOException, ConverterException {
        assertConvertedSame("${x + 1\r\n\t- y % 2 / 2 * +z / -1}");
        assertConvertedSame("${x * (y + z) * (\ty+z\n)}");

        assertConvertedSame("${f()}");
        assertConvertedSame("${f(1)}");
        assertConvertedSame("${f(1, 2)}");
        assertConvertedSame("${f<#-- C1 -->(<#-- C2 --> 1, 2 ,<#-- C3 --> 3,<#-- C4 -->4 <#-- C5 -->)}");
    }

    @Test
    public void testDirectives() throws IOException, ConverterException {
        assertConvertedSame("<#if foo>1</#if>");
        assertConvertedSame("<#if\n  foo\n>\n123\n</#if\n>");

        assertConverted("<#if foo>1<#elseIf bar>2<#else>3</#if>", "<#if foo>1<#elseif bar>2<#else>3</#if>");
        assertConvertedSame("<#if  foo >1<#elseIf  bar >2<#else >3</#if >");
    }

    @Test
    public void testBuiltInExpressions() throws IOException, ConverterException {
        assertConverted("${s?upperCase} ${s?leftPad(123)}", "${s?upper_case} ${s?left_pad(123)}");
        assertConverted("${s?html}", "${s?web_safe}");
        assertConvertedSame("${s  ?   upperCase\t?\t\tleftPad(5)}");
    }

    @Test
    public void testComments() throws IOException, ConverterException {
        assertConvertedSame("\n<#--\n  c\n\t-->\n");
        assertConvertedSame("${1 + <#-- C1 -->\r\n2 +[#-- C2 --]3 +<!--\tC3\t-->4 +[!-- C4 --] 5 + -<!-- -->1}");
    }

    @Test
    public void testSquareBracketTagSyntax() throws IOException, ConverterException {
        assertConvertedSame("[#if true <#-- c -->[#-- c --]]${v}[/#if]", true);
    }

    @Test
    public void testFileExtensions() throws IOException, ConverterException {
        FileUtils.write(new File(srcDir, "t1"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t2.foo"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t3.ftl"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t4.ftlh"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t5.ftlx"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t6.ftl"), "[#ftl]", UTF_8);
        FileUtils.write(new File(srcDir, "t7.ftlh"), "[#ftl]", UTF_8);
        FileUtils.write(new File(srcDir, "t8.ftlx"), "[#ftl]", UTF_8);
        FileUtils.write(new File(srcDir, "t9.Ftl"), "x", UTF_8);
        FileUtils.write(new File(srcDir, "t10.Foo3"), "x", UTF_8);

        FM2ToFM3Converter converter = new FM2ToFM3Converter();
        converter.setSource(srcDir);
        converter.setDestinationDirectory(dstDir);
        Properties properties = new Properties();
        properties.setProperty(Configuration.DEFAULT_ENCODING_KEY, UTF_8.name());
        converter.setFreeMarker2Settings(properties);

        converter.execute();

        assertTrue(new File(dstDir, "t1").exists());
        assertTrue(new File(dstDir, "t2.foo").exists());
        assertTrue(new File(dstDir, "t3.fm3").exists());
        assertTrue(new File(dstDir, "t4.fm3h").exists());
        assertTrue(new File(dstDir, "t5.fm3x").exists());
        assertTrue(new File(dstDir, "t6.fm3s").exists());
        assertTrue(new File(dstDir, "t7.fm3sh").exists());
        assertTrue(new File(dstDir, "t8.fm3sx").exists());
        assertTrue(new File(dstDir, "t9.fm3").exists());
        assertTrue(new File(dstDir, "t10.Foo3").exists());
    }

    private void assertConvertedSame(String ftl2) throws IOException, ConverterException {
        assertConverted(ftl2, ftl2);
    }

    private void assertConverted(String ftl3, String ftl2) throws IOException, ConverterException {
        assertEquals(ftl3, convert(ftl2));
    }

    private void assertConvertedSame(String ftl2, boolean squareBracketTagSyntax)
            throws IOException, ConverterException {
        assertConverted(ftl2, ftl2, squareBracketTagSyntax);
    }

    private void assertConverted(String ftl3, String ftl2, boolean squareBracketTagSyntax)
            throws IOException, ConverterException {
        assertEquals(ftl3, convert(ftl2, squareBracketTagSyntax));
    }

    private String convert(String ftl2) throws IOException, ConverterException {
        return convert(ftl2, false);
    }

    private String convert(String ftl2, boolean squareBracketTagSyntax) throws IOException, ConverterException {
        File srcFile = new File(srcDir, "t");
        FileUtils.write(srcFile, ftl2, UTF_8);

        FM2ToFM3Converter converter = new FM2ToFM3Converter();
        converter.setSource(srcFile);
        converter.setDestinationDirectory(dstDir);
        Properties properties = new Properties();
        properties.setProperty(Configuration.DEFAULT_ENCODING_KEY, UTF_8.name());
        if (squareBracketTagSyntax) {
            properties.setProperty(Configuration.TAG_SYNTAX_KEY, "squareBracket");
        }
        converter.setFreeMarker2Settings(properties);

        converter.execute();

        File outputFile = new File(dstDir, "t");
        String output = FileUtils.readFileToString(outputFile, UTF_8);
        if (!outputFile.delete()) {
            throw new IOException("Couldn't delete file: " + outputFile);
        }

        return output;
    }

}
