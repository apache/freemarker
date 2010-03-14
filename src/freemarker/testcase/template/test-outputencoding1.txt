Output charset: ${.output_encoding?default("undefined")}
URL escaping charset: ${.url_escaping_charset?default("undefined")}

<#assign s="a/%b">
<#setting url_escaping_charset="UTF-16">
${s?url}
${s?url}
<#setting url_escaping_charset="ISO-8859-1">
${s?url}
${s?url}
${s?url('UTF-16')}
${s?url}