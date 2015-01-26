/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.StrongCacheStorage;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;

public class TemplateLevelSettings {
    
    private static final String IMPORTED_FTL = "imported.ftl";
    private static final String INCLUDED_FTL = "included.ftl";
    private static final String MAIN_FTL = "main.ftl";
    private static final StringTemplateLoader TEMPLATES = new StringTemplateLoader();
    static {
        TEMPLATES.putTemplate(MAIN_FTL,
                "${true}<#include '" + INCLUDED_FTL +"'>"
                + "${true}<#import '" + IMPORTED_FTL +"' as ns>"
                + "${true}<@ns.impM1>${true}</@>"
                + "${true}<@incM>${true}</@>"
                + "${true}");
        TEMPLATES.putTemplate(INCLUDED_FTL,
                "[inc:${true}]"
                + "<#macro incM>[incM:${true}{<#nested>}${true}]</#macro>");
        TEMPLATES.putTemplate(IMPORTED_FTL,
                "<#macro impM1>[impM1:${true}{<#nested>}${true}<@impM2>${true}</@>${true}]</#macro>"
                + "<#macro impM2>[impM2:${true}{<#nested>}${true}]</#macro>"
                );
    }
    
    @Test
    public void test() throws IOException, TemplateException {
        assertOutputs(
                "M[inc:INC]MM[impM1:M{M}M[impM2:M{IMP}M]M]M[incM:M{M}M]M",
                "M[inc:M]MM[impM1:M{M}M[impM2:M{M}M]M]M[incM:M{M}M]M",
                "M,m", "INC,inc", "IMP,imp");
        assertOutputs(
                "C[inc:INC]CC[impM1:C{C}C[impM2:C{IMP}C]C]C[incM:C{C}C]C",
                "C[inc:C]CC[impM1:C{C}C[impM2:C{C}C]C]C[incM:C{C}C]C",
                null, "INC,inc", "IMP,imp");
        assertOutputs(
                "M[inc:C]MM[impM1:M{M}M[impM2:M{IMP}M]M]M[incM:M{M}M]M",
                "M[inc:M]MM[impM1:M{M}M[impM2:M{M}M]M]M[incM:M{M}M]M",
                "M,m", null, "IMP,imp");
        assertOutputs(
                "M[inc:INC]MM[impM1:M{M}M[impM2:M{C}M]M]M[incM:M{M}M]M",
                "M[inc:M]MM[impM1:M{M}M[impM2:M{M}M]M]M[incM:M{M}M]M",
                "M,m", "INC,inc", null);
    }

    private void assertOutputs(
            String legacyOutput, String fixedOutput,
            String mainBoolFmt, String incBoolFmt, String impBoolFtm)
            throws IOException, TemplateException {
        assertEquals(
                legacyOutput,
                renderWith(Configuration.VERSION_2_3_21, mainBoolFmt, incBoolFmt, impBoolFtm));
        assertEquals(
                fixedOutput,
                renderWith(Configuration.VERSION_2_3_22, mainBoolFmt, incBoolFmt, impBoolFtm));
    }
    
    private String renderWith(Version version, String mainBoolFmt, String incBoolFmt, String impBoolFtm)
            throws IOException, TemplateException {
        Configuration cfg = new Configuration(version);
        cfg.setTemplateLoader(TEMPLATES);
        cfg.setCacheStorage(new StrongCacheStorage());
        cfg.setBooleanFormat("C,c");
        
        if (incBoolFmt != null) {
            cfg.getTemplate(INCLUDED_FTL).setBooleanFormat(incBoolFmt);
        }
        
        if (impBoolFtm != null) {
            cfg.getTemplate(IMPORTED_FTL).setBooleanFormat(impBoolFtm);
        }
        
        Template t = cfg.getTemplate(MAIN_FTL);
        if (mainBoolFmt != null) {
            t.setBooleanFormat(mainBoolFmt);
        }
        
        StringWriter sw = new StringWriter();
        t.process(null, sw);
        return sw.toString();
    }

}
