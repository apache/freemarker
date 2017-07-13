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
<@assertEquals actual=0?abs expected=0 />
<@assertEquals actual=3?abs expected=3 />
<@assertEquals actual=(-3)?abs expected=3 />
<@assertEquals actual=3.5?abs expected=3.5 />
<@assertEquals actual=(-3.5)?abs expected=3.5 />

<@assert test=fNan?abs?isNan />
<@assert test=dNan?abs?isNan />
<@assert test=fNinf?abs?isInfinite />
<@assert test=dPinf?abs?isInfinite />
<@assert test=fNinf lt 0 />
<@assert test=dPinf gt 0 />
<@assert test=fNinf?abs gt 0 />
<@assert test=dPinf?abs gt 0 />

<@assertEquals actual=fn?abs expected=0.05 />
<@assertEquals actual=dn?abs expected=0.05 />
<@assertEquals actual=ineg?abs expected=5 />
<@assertEquals actual=ln?abs expected=5 />
<@assertEquals actual=sn?abs expected=5 />
<@assertEquals actual=bn?abs expected=5 />
<@assertEquals actual=bin?abs expected=5 />
<@assertEquals actual=bdn?abs expected=0.05 />

<@assertEquals actual=fp?abs expected=0.05 />
<@assertEquals actual=dp?abs expected=0.05 />
<@assertEquals actual=ip?abs expected=5 />
<@assertEquals actual=lp?abs expected=5 />
<@assertEquals actual=sp?abs expected=5 />
<@assertEquals actual=bp?abs expected=5 />
<@assertEquals actual=bip?abs expected=5 />
<@assertEquals actual=bdp?abs expected=0.05 />

<@assert test=!0?isInfinite />
<@assert test=!fn?isInfinite />
<@assert test=!dn?isInfinite />
<@assert test=!ineg?isInfinite />
<@assert test=!ln?isInfinite />
<@assert test=!sn?isInfinite />
<@assert test=!bn?isInfinite />
<@assert test=!bin?isInfinite />
<@assert test=!bdn?isInfinite />
<@assert test=!fNan?isInfinite />
<@assert test=!dNan?isInfinite />
<@assert test=fNinf?isInfinite />
<@assert test=dPinf?isInfinite />

<@assert test=!0?isNan />
<@assert test=!fn?isNan />
<@assert test=!dn?isNan />
<@assert test=!ineg?isNan />
<@assert test=!ln?isNan />
<@assert test=!sn?isNan />
<@assert test=!bn?isNan />
<@assert test=!bin?isNan />
<@assert test=!bdn?isNan />
<@assert test=fNan?isNan />
<@assert test=dNan?isNan />
<@assert test=!fNinf?isNan />
<@assert test=!dPinf?isNan />