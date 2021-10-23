<#--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<#assign mo1 = "Foo's bar {}"?esc>
HTML: ${mo1}
XML:  <#outputformat 'XML'>${mo1}</#outputformat>
RTF:  <#outputformat 'RTF'>${mo1}</#outputformat>

<#assign mo2><p>Test</#assign>
HTML: ${mo2}
XML:  <#attempt><#outputformat 'XML'>${mo2}</#outputformat><#recover>Failed</#attempt>
RTF:  <#attempt><#outputformat 'RTF'>${mo2}</#outputformat><#recover>Failed</#attempt>
