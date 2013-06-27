<@assertEquals actual=0?abs expected=0 />
<@assertEquals actual=3?abs expected=3 />
<@assertEquals actual=(-3)?abs expected=3 />
<@assertEquals actual=3.5?abs expected=3.5 />
<@assertEquals actual=(-3.5)?abs expected=3.5 />

<@assert test=fNan?abs?is_nan />
<@assert test=dNan?abs?is_nan />
<@assert test=fNinf?abs?is_infinite />
<@assert test=dPinf?abs?is_infinite />
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

<@assert test=!0?is_infinite />
<@assert test=!fn?is_infinite />
<@assert test=!dn?is_infinite />
<@assert test=!ineg?is_infinite />
<@assert test=!ln?is_infinite />
<@assert test=!sn?is_infinite />
<@assert test=!bn?is_infinite />
<@assert test=!bin?is_infinite />
<@assert test=!bdn?is_infinite />
<@assert test=!fNan?is_infinite />
<@assert test=!dNan?is_infinite />
<@assert test=fNinf?is_infinite />
<@assert test=dPinf?is_infinite />

<@assert test=!0?is_nan />
<@assert test=!fn?is_nan />
<@assert test=!dn?is_nan />
<@assert test=!ineg?is_nan />
<@assert test=!ln?is_nan />
<@assert test=!sn?is_nan />
<@assert test=!bn?is_nan />
<@assert test=!bin?is_nan />
<@assert test=!bdn?is_nan />
<@assert test=fNan?is_nan />
<@assert test=dNan?is_nan />
<@assert test=!fNinf?is_nan />
<@assert test=!dPinf?is_nan />