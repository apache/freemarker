<#assign ef = JspTaglibs["http://freemarker.org/test/taglibs/el-functions"]>

${ef.reverse("abc")}
${ef.reverseInt(123)}
${ef.reverseIntRadix(123, 2)}
${ef.hypotenuse(3, 4)}
${ef.sum(ef.testArray())}
${ef.sum([1, 2, 3])}
${ef.sum(1, 2, 3)}
${ef.sum(1)}
${ef.sumMap(ef.testMap())}
${ef.sumMap({ 'a': 1?int, 'b': 2?int, 'c': 3?int })}