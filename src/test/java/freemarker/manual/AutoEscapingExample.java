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
package freemarker.manual;

import org.junit.Test;

public class AutoEscapingExample extends ExamplesTest {

    @Test
    public void testInfoBox() throws Exception {
        assertOutputForNamed("AutoEscapingExample-infoBox.ftlh");
    }

    @Test
    public void testCapture() throws Exception {
        assertOutputForNamed("AutoEscapingExample-capture.ftlh");
    }

    @Test
    public void testMarkup() throws Exception {
        assertOutputForNamed("AutoEscapingExample-markup.ftlh");
    }

    @Test
    public void testConvert() throws Exception {
        assertOutputForNamed("AutoEscapingExample-convert.ftlh");
    }

    @Test
    public void testConvert2() throws Exception {
        assertOutputForNamed("AutoEscapingExample-convert2.ftl");
    }

    @Test
    public void testStringLiteral() throws Exception {
        assertOutputForNamed("AutoEscapingExample-stringLiteral.ftlh");
    }

    @Test
    public void testStringLiteral2() throws Exception {
        assertOutputForNamed("AutoEscapingExample-stringLiteral2.ftlh");
    }

    @Test
    public void testStringConcat() throws Exception {
        assertOutputForNamed("AutoEscapingExample-stringConcat.ftlh");
    }
    
}
