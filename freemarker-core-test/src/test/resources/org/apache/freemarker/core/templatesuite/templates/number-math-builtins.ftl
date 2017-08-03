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

<@assert fNan?abs?isNan />
<@assert dNan?abs?isNan />
<@assert fNinf?abs?isInfinite />
<@assert dPinf?abs?isInfinite />
<@assert fNinf lt 0 />
<@assert dPinf gt 0 />
<@assert fNinf?abs gt 0 />
<@assert dPinf?abs gt 0 />

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

<@assert !0?isInfinite />
<@assert !fn?isInfinite />
<@assert !dn?isInfinite />
<@assert !ineg?isInfinite />
<@assert !ln?isInfinite />
<@assert !sn?isInfinite />
<@assert !bn?isInfinite />
<@assert !bin?isInfinite />
<@assert !bdn?isInfinite />
<@assert !fNan?isInfinite />
<@assert !dNan?isInfinite />
<@assert fNinf?isInfinite />
<@assert dPinf?isInfinite />

<@assert !0?isNan />
<@assert !fn?isNan />
<@assert !dn?isNan />
<@assert !ineg?isNan />
<@assert !ln?isNan />
<@assert !sn?isNan />
<@assert !bn?isNan />
<@assert !bin?isNan />
<@assert !bdn?isNan />
<@assert fNan?isNan />
<@assert dNan?isNan />
<@assert !fNinf?isNan />
<@assert !dPinf?isNan />