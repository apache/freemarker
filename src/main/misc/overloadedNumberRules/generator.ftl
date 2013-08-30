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
