<@assertEquals expected="" actual=""+false />
<@assertEquals expected="true" actual=""+true />
<@assertEquals expected="false" actual=false?string />  <#-- In 2.1 bool?string was error, now it does what 2.3 does -->
<@assertEquals expected="n" actual=false?string('y', 'n') />
<@assertEquals expected="false" actual=""+beanFalse />
<@assertEquals expected="true" actual=""+beanTrue />
<@assertEquals expected="false" actual=beanFalse?string />
<@assertEquals expected="n" actual=beanFalse?string('y', 'n') />
