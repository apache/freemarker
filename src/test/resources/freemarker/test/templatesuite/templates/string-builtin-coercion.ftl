Was broken 2.3.19:
<#setting number_format="0.#">
<@assert test=1232?contains('2') />
<@assert test=1232?index_of('2') == 1 />
<@assert test=1232?last_index_of('2') == 3 />
<@assert test=1232?left_pad(6) == '  1232' /><@assert test=1232?left_pad(6, '0') == '001232' />
<@assert test=1232?right_pad(6) == '1232  ' /><@assert test=1232?right_pad(6, '0') == '123200' />
<@assert test=1232?matches('[1-3]+') />
<@assert test=1232?replace('2', 'z') == '1z3z' />
<@assert test=1232?replace('2', 'z', 'r') == '1z3z' />
<@assert test=1232?split('2')[1] == '3' /><@assert test=1232?split('2')[2] == '' />
<@assert test=1232?split('2', 'r')[1] == '3' />

Was no broken in 2.3.19:
<@assert test=1232?starts_with('12') />
<@assert test=1232?ends_with('32') />
