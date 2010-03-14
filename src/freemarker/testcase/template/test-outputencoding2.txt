Output charset: ${.output_encoding?default("undefined")}
URL escaping charset: ${.url_escaping_charset?default("undefined")}

<#assign s="a/%b">
UTF-16: ${s?url}
ISO-8859-1: ${s?url('ISO-8859-1')}
UTF-16: ${s?url}
<#setting url_escaping_charset="ISO-8859-1">
ISO-8859-1: ${s?url}
ISO-8859-1: ${s?url}