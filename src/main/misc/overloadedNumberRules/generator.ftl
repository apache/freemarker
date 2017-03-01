<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
    static int getArgumentConversionPrice(Class fromC, Class toC) {
        // DO NOT EDIT, generated code!
        // See: src\main\misc\overloadedNumberRules\README.txt
        if (toC == fromC) {
            return 0;
        <#list toCsFreqSorted as toC><#t>
        } else if (toC == ${toC}.class) {
                <#assign firstFromC = true>
                <#list fromCsFreqSorted as fromC>
                    <#if toC != fromC>
            <#assign row = []>
            <#list t as i>
                <#if i[0] == fromC>
                    <#assign row = i>
                    <#break>
                </#if>
            </#list>
            <#if !row?has_content><#stop "Not found: " + fromC></#if>
            <#if !firstFromC>else </#if>if (fromC == ${fromC}.class) return ${toPrice(row[toC], toCsCostBoosts[toC])};
            <#assign firstFromC = false>
                    </#if>
                </#list>
            else return Integer.MAX_VALUE;
        </#list>
        } else {
            // Unknown toC; we don't know how to convert to it:
            return Integer.MAX_VALUE;
        }        
    }

    static int compareNumberTypeSpecificity(Class c1, Class c2) {
        // DO NOT EDIT, generated code!
        // See: src\main\misc\overloadedNumberRules\README.txt
        c1 = ClassUtil.primitiveClassToBoxingClass(c1);
        c2 = ClassUtil.primitiveClassToBoxingClass(c2);
        
        if (c1 == c2) return 0;
        
        <#list toCsFreqSorted as c1><#t>
        if (c1 == ${c1}.class) {
          <#list toCsFreqSorted as c2><#if c1 != c2><#t>
            if (c2 == ${c2}.class) return ${toCsCostBoosts[c2]} - ${toCsCostBoosts[c1]};
          </#if></#list>
            return 0;
        }
        </#list>
        return 0;
    }

<#function toPrice cellValue, boost>
    <#if cellValue?starts_with("BC ")>
        <#local cellValue = cellValue[3..]>
    <#elseif cellValue == '-' || cellValue == 'N/A'>
        <#return 'Integer.MAX_VALUE'>
    </#if>
    <#local cellValue = cellValue?number>
    <#if cellValue != 0>
        <#return cellValue * 10000 + boost>
    <#else>
        <#return 0>
    </#if>
</#function>
