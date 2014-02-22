<@assertEquals expected="string" actual=mStringC.c />
<@assertEquals expected=1 actual=mStringC?keys?size />
<@assertEquals expected="null" actual=mStringC.d!'null' />
<@assertEquals expected=1 actual=mStringC?keys?size />

<@assertEquals expected="null" actual=mStringCNull.c!'null' />
<@assertEquals expected=1 actual=mStringCNull?keys?size />
<@assertEquals expected="null" actual=mStringCNull.d!'null' />
<@assertEquals expected=1 actual=mStringCNull?keys?size />

<@assertEquals expected="char" actual=mCharC.c />
<@assertEquals expected=1 actual=mCharC?keys?size />
<@assertEquals expected="null" actual=mCharC.d!'null' />
<@assertEquals expected=1 actual=mCharC?keys?size />

<@assertEquals expected="null" actual=mCharCNull.c!'null' />
<@assertEquals expected=1 actual=mCharCNull?keys?size />
<@assertEquals expected="null" actual=mCharCNull.d!'null' />
<@assertEquals expected=1 actual=mCharCNull?keys?size />

<@assertEquals expected="char" actual=mMixed.c />
<@assertEquals expected="string" actual=mMixed.s />
<@assertEquals expected="string2" actual=mMixed.s2 />
<@assertEquals expected="null" actual=mMixed.s2n!'null' />
<@assertEquals expected="null" actual=mMixed.wrong!'null' />
<@assertEquals expected=4 actual=mMixed?keys?size />
