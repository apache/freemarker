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
package org.apache.freemarker.core;

import static org.junit.Assert.*;

import java.util.Locale;

import org.apache.freemarker.core.util._LocaleUtils;
import org.junit.Test;

public class CoreLocaleUtilsTest {

    @Test
    public void testGetLessSpecificLocale() {
        Locale locale;
        
        locale = new Locale("ru", "RU", "Linux");
        assertEquals("ru_RU_Linux", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertEquals("ru_RU", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertEquals("ru", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertNull(locale);
        
        locale = new Locale("ch", "CH");
        assertEquals("ch_CH", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertEquals("ch", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertNull(locale);
        
        locale = new Locale("ja");
        assertEquals("ja", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertNull(locale);

        locale = new Locale("ja", "", "");
        assertEquals("ja", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertNull(locale);
        
        locale = new Locale("");
        assertEquals("", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertNull(locale);
        
        locale = new Locale("hu", "", "Linux");
        assertEquals("hu__Linux", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertEquals("hu", locale.toString());
        locale = _LocaleUtils.getLessSpecificLocale(locale);
        assertNull(locale);
    }
    
}
