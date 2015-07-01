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

import java.io.IOException;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

/**
 * The {@code interpret} built-in must not consider the settings or established auto-detected syntax of the surrounding
 * template. It can only depend on the {@link Configuration}.
 */
public class InterpretSettingInheritanceTest  extends TemplateTest {

    private static final String FTL_A_S_A = "<#ftl><@'[#if true]s[/#if]<#if true>a</#if>'?interpret />";
    private static final String FTL_A_A_S = "<#ftl><@'<#if true>a</#if>[#if true]s[/#if]'?interpret />";
    private static final String FTL_S_S_A = "[#ftl][@'[#if true]s[/#if]<#if true>a</#if>'?interpret /]";
    private static final String FTL_S_A_S = "[#ftl][@'<#if true>a</#if>[#if true]s[/#if]'?interpret /]";
    private static final String OUT_S_A_WHEN_SYNTAX_IS_S = "s<#if true>a</#if>";
    private static final String OUT_S_A_WHEN_SYNTAX_IS_A = "[#if true]s[/#if]a";
    private static final String OUT_A_S_WHEN_SYNTAX_IS_A = "a[#if true]s[/#if]";
    private static final String OUT_A_S_WHEN_SYNTAX_IS_S = "<#if true>a</#if>s";

    @Test
    public void tagSyntaxTest() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        
        cfg.setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertOutput(FTL_S_A_S, OUT_A_S_WHEN_SYNTAX_IS_A);
        assertOutput(FTL_S_S_A, OUT_S_A_WHEN_SYNTAX_IS_A);
        assertOutput(FTL_A_A_S, OUT_A_S_WHEN_SYNTAX_IS_A);
        assertOutput(FTL_A_S_A, OUT_S_A_WHEN_SYNTAX_IS_A);
        
        cfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        assertOutput(FTL_S_A_S, OUT_A_S_WHEN_SYNTAX_IS_S);
        assertOutput(FTL_S_S_A, OUT_S_A_WHEN_SYNTAX_IS_S);
        assertOutput(FTL_A_A_S, OUT_A_S_WHEN_SYNTAX_IS_S);
        assertOutput(FTL_A_S_A, OUT_S_A_WHEN_SYNTAX_IS_S);
        
        cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
        assertOutput(FTL_S_A_S, OUT_A_S_WHEN_SYNTAX_IS_A);
        assertOutput(FTL_S_S_A, OUT_S_A_WHEN_SYNTAX_IS_S);
        assertOutput(FTL_A_A_S, OUT_A_S_WHEN_SYNTAX_IS_A);
        assertOutput(FTL_A_S_A, OUT_S_A_WHEN_SYNTAX_IS_S);
        assertOutput("<@'[#ftl]x'?interpret />[#if true]y[/#if]", "x[#if true]y[/#if]");
    }

    @Test
    public void whitespaceStrippingTest() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        
        cfg.setWhitespaceStripping(true);
        assertOutput("<#assign x = 1>\nX<@'<#assign x = 1>\\nY'?interpret />", "XY");
        assertOutput("<#ftl stripWhitespace=false><#assign x = 1>\nX<@'<#assign x = 1>\\nY'?interpret />", "\nXY");
        assertOutput("<#assign x = 1>\nX<@'<#ftl stripWhitespace=false><#assign x = 1>\\nY'?interpret />", "X\nY");
        
        cfg.setWhitespaceStripping(false);
        assertOutput("<#assign x = 1>\nX<@'<#assign x = 1>\\nY'?interpret />", "\nX\nY");
        assertOutput("<#ftl stripWhitespace=true><#assign x = 1>\nX<@'<#assign x = 1>\\nY'?interpret />", "X\nY");
        assertOutput("<#assign x = 1>\nX<@'<#ftl stripWhitespace=true><#assign x = 1>\\nY'?interpret />", "\nXY");
    }

    @Test
    public void evalTest() throws IOException, TemplateException {
        Configuration cfg = getConfiguration();
        
        cfg.setTagSyntax(Configuration.ANGLE_BRACKET_TAG_SYNTAX);
        assertOutput("<@'\"[#if true]s[/#if]<#if true>a</#if>\"?interpret'?eval />", OUT_S_A_WHEN_SYNTAX_IS_A);
        assertOutput("[#ftl][@'\"[#if true]s[/#if]<#if true>a</#if>\"?interpret'?eval /]", OUT_S_A_WHEN_SYNTAX_IS_A);
        
        cfg.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
        assertOutput("[@'\"[#if true]s[/#if]<#if true>a</#if>\"?interpret'?eval /]", OUT_S_A_WHEN_SYNTAX_IS_S);
        assertOutput("<#ftl><@'\"[#if true]s[/#if]<#if true>a</#if>\"?interpret'?eval />", OUT_S_A_WHEN_SYNTAX_IS_S);
    }
    
}
