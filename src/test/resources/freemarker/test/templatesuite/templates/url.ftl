<#setting url_escaping_charset="utf-8">
<#assign s = 'a/báb?c/x;y=1' />
<@assertEquals expected='a%2Fb%E1b%3Fc%2Fx%3By%3D1' actual=s?url('ISO-8859-1') />
<@assertEquals expected='a%2Fb%C3%A1b%3Fc%2Fx%3By%3D1' actual=s?url />
<@assertEquals expected='a/b%E1b%3Fc/x%3By%3D1' actual=s?url_path('ISO-8859-1') />
<@assertEquals expected='a/b%C3%A1b%3Fc/x%3By%3D1' actual=s?url_path />