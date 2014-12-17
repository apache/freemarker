<#assign ef = JspTaglibs["http://freemarker.org/test/taglibs/el-functions"]>

${ef.reverse("abc")}
${ef.reverseInt(123)}
${ef.reverseIntRadix(123, 2)}
${ef.hypotenuse(3, 4)}
