<#ftl output_format="HTML"><#-- Needed for the Eclipse plugin -->

<@infoBox "Foo & bar" />
<@infoBox "Foo <b>bar</b>"?no_esc />

<#macro infoBox message>
  <div class="infoBox">
    ${message}
  </div>
</#macro>