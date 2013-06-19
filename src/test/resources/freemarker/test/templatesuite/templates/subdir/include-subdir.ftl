<p>This is include-subdir.ftl</p>
<p>Testing including from same directory</p>
<#include "include-subdir2.ftl">
<p>Testing including from relative parent</p>
<#include "../included.ftl">
<p>Testing including from loader root</p>
<#include "/included.ftl">
<p>Testing including through acquisition</p>
<#include "*/subdir/include-subdir2.ftl">