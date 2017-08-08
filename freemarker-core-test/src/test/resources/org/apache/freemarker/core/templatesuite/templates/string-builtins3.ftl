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
<@assertEquals expected='foo' actual='foo'?keepBefore('x') />
<@assertEquals expected='f' actual='foo'?keepBefore('o') />
<@assertEquals expected='' actual='foo'?keepBefore('f') />
<@assertEquals expected='fo' actual='foobar'?keepBefore('ob') />
<@assertEquals expected='foob' actual='foobar'?keepBefore('ar') />
<@assertEquals expected='' actual='foobar'?keepBefore('foobar') />
<@assertEquals expected='' actual='foobar'?keepBefore('') />
<@assertEquals expected='' actual='foobar'?keepBefore('', 'r') />
<@assertEquals expected='FOO' actual='FOO'?keepBefore('o') />
<@assertEquals expected='F' actual='FOO'?keepBefore('o', 'i') />
<@assertEquals expected='fo' actual='fo.o'?keepBefore('.') />
<@assertEquals expected='' actual='fo.o'?keepBefore('.', 'r') />
<@assertEquals expected='FOOb' actual='FOObaar'?keepBefore(r'([a-z])\1', 'r') />
<@assertEquals expected='F' actual='FOObaar'?keepBefore(r'([a-z])\1', 'ri') />
<@assertEquals expected='foo' actual="foo : bar"?keepBefore(r"\s*:\s*", "r") />
<@assertEquals expected='foo' actual="foo:bar"?keepBefore(r"\s*:\s*", "r") />
<@assertFails message='"m" flag'>
    ${'x'?keepBefore('x', 'm')}
</@assertFails>
<@assertFails message='can only have'>
    ${'x'?keepBefore('x', 'i', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?keepBefore()}
</@assertFails>

<@assertEquals expected='' actual=''?keepBeforeLast('f') />
<@assertEquals expected='ff' actual='fff'?keepBeforeLast('f') />
<@assertEquals expected='' actual='foo'?keepBeforeLast('f') />
<@assertEquals expected='' actual='f'?keepBeforeLast('f') />
<@assertEquals expected='a.b' actual='a.b.txt'?keepBeforeLast('.') />
<@assertEquals expected='ab' actual='ab'?keepBeforeLast('.') />
<@assertEquals expected='a' actual='ab'?keepBeforeLast('.', 'r') />
<@assertEquals expected='ab' actual='ab'?keepBeforeLast(r'\.', 'r') />
<@assertEquals expected='af' actual='afFf'?keepBeforeLast('F') />
<@assertEquals expected='afF' actual='afFf'?keepBeforeLast('F', 'i') />
<@assertEquals expected='1a2' actual='1a2b3'?keepBeforeLast('[ab]', 'r') />
<@assertEquals expected='aa' actual='aaabb'?keepBeforeLast('[ab]{3}', 'r') />
<@assertEquals expected='aaabbx' actual='aaabbxbabe'?keepBeforeLast('[ab]{3}', 'r') />
<@assertEquals expected='xxxaa' actual='xxxaaayyy'?keepBeforeLast('a+', 'r') />
<@assertEquals expected='foobar' actual='foobar'?keepBeforeLast('') />
<@assertEquals expected='foobar' actual='foobar'?keepBeforeLast('', 'r') />
<@assertFails message='"m" flag'>
    ${'x'?keepBeforeLast('x', 'm')}
</@assertFails>
<@assertFails message='can only have'>
    ${'x'?keepBeforeLast('x', 'i', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?keepBeforeLast()}
</@assertFails>

<@assertEquals expected='' actual='foo'?keepAfter('x') />
<@assertEquals expected='o' actual='foo'?keepAfter('o') />
<@assertEquals expected='oo' actual='foo'?keepAfter('f') />
<@assertEquals expected='ar' actual='foobar'?keepAfter('ob') />
<@assertEquals expected='' actual='foobar'?keepAfter('ar') />
<@assertEquals expected='' actual='foobar'?keepAfter('foobar') />
<@assertEquals expected='foobar' actual='foobar'?keepAfter('') />
<@assertEquals expected='foobar' actual='foobar'?keepAfter('', 'r') />
<@assertEquals expected='' actual='FOO'?keepAfter('o') />
<@assertEquals expected='O' actual='FOO'?keepAfter('o', 'i') />
<@assertEquals expected='o' actual='fo.o'?keepAfter('.') />
<@assertEquals expected='o.o' actual='fo.o'?keepAfter('.', 'r') />
<@assertEquals expected='r' actual='FOObaar'?keepAfter(r'([a-z])\1', 'r') />
<@assertEquals expected='baar' actual='FOObaar'?keepAfter(r'([a-z])\1', 'ri') />
<@assertEquals expected='bar' actual="foo : bar"?keepAfter(r"\s*:\s*", "r") />
<@assertEquals expected='bar' actual="foo:bar"?keepAfter(r"\s*:\s*", "r") />
<@assertFails message='"m" flag'>
    ${'x'?keepAfter('x', 'm')}
</@assertFails>
<@assertFails message='can only have'>
    ${'x'?keepAfter('x', 'i', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?keepAfter()}
</@assertFails>

<@assertEquals expected='' actual=''?keepAfterLast('f') />
<@assertEquals expected='' actual='fff'?keepAfterLast('f') />
<@assertEquals expected='' actual='oof'?keepAfterLast('f') />
<@assertEquals expected='' actual='f'?keepAfterLast('f') />
<@assertEquals expected='txt' actual='a.b.txt'?keepAfterLast('.') />
<@assertEquals expected='' actual='ab'?keepAfterLast('.') />
<@assertEquals expected='' actual='ab'?keepAfterLast('.', 'r') />
<@assertEquals expected='' actual='ab'?keepAfterLast(r'\.', 'r') />
<@assertEquals expected='fa' actual='fFfa'?keepAfterLast('F') />
<@assertEquals expected='a' actual='fFfa'?keepAfterLast('F', 'i') />
<@assertEquals expected='3' actual='1a2b3'?keepAfterLast('[ab]', 'r') />
<@assertEquals expected='' actual='aaabb'?keepAfterLast('[ab]{3}', 'r') />
<@assertEquals expected='x' actual='aaabbx'?keepAfterLast('[ab]{3}', 'r') />
<@assertEquals expected='e' actual='aaabbxbabe'?keepAfterLast('[ab]{3}', 'r') />
<@assertEquals expected='12345' actual='aaabb12345'?keepAfterLast('[ab]{3}', 'r') />
<@assertEquals expected='yyy' actual='xxxaaayyy'?keepAfterLast('a+', 'r') />
<@assertEquals expected='' actual='foobar'?keepAfterLast('') />
<@assertEquals expected='' actual='foobar'?keepAfterLast('', 'r') />
<@assertFails message='"m" flag'>
    ${'x'?keepAfterLast('x', 'm')}
</@assertFails>
<@assertFails message='can only have'>
    ${'x'?keepAfterLast('x', 'i', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?keepAfterLast()}
</@assertFails>

<@assertEquals expected='foo' actual='foo'?removeBeginning('x') />
<@assertEquals expected='foo' actual='foo'?removeBeginning('o') />
<@assertEquals expected='foo' actual='foo'?removeBeginning('fooo') />
<@assertEquals expected='oo' actual='foo'?removeBeginning('f') />
<@assertEquals expected='o' actual='foo'?removeBeginning('fo') />
<@assertEquals expected='' actual='foo'?removeBeginning('foo') />
<@assertEquals expected='foo' actual='foo'?removeBeginning('') />
<@assertFails message='can only have'>
    ${'x'?removeBeginning('x', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?removeBeginning()}
</@assertFails>

<@assertEquals expected='bar' actual='bar'?removeEnding('x') />
<@assertEquals expected='bar' actual='bar'?removeEnding('a') />
<@assertEquals expected='bar' actual='bar'?removeEnding('barr') />
<@assertEquals expected='ba' actual='bar'?removeEnding('r') />
<@assertEquals expected='b' actual='bar'?removeEnding('ar') />
<@assertEquals expected='' actual='bar'?removeEnding('bar') />
<@assertEquals expected='bar' actual='bar'?removeEnding('') />
<@assertFails message='can only have'>
    ${'x'?removeEnding('x', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?removeEnding()}
</@assertFails>

<@assertEquals expected='xfoo' actual='foo'?ensureStartsWith('x') />
<@assertEquals expected='foo' actual='foo'?ensureStartsWith('f') />
<@assertEquals expected='foo' actual='foo'?ensureStartsWith('foo') />
<@assertEquals expected='fooofoo' actual='foo'?ensureStartsWith('fooo') />
<@assertEquals expected='foo' actual='foo'?ensureStartsWith('') />
<@assertEquals expected='x' actual=''?ensureStartsWith('x') />
<@assertEquals expected='' actual=''?ensureStartsWith('') />
<@assertEquals expected='bacdef' actual="bacdef"?ensureStartsWith("[ab]{2}", "ab") />
<@assertEquals expected='bacdef' actual="bacdef"?ensureStartsWith("^[ab]{2}", "ab") />
<@assertEquals expected='abcacdef' actual="cacdef"?ensureStartsWith("[ab]{2}", "ab") />
<@assertEquals expected='abcacdef' actual="cacdef"?ensureStartsWith("^[ab]{2}", "ab") />
<@assertEquals expected='ab!cdef' actual="cdef"?ensureStartsWith("ab", "ab!") />
<@assertEquals expected='ab!ABcdef' actual="ABcdef"?ensureStartsWith("ab", "ab!") />
<@assertEquals expected='ABcdef' actual="ABcdef"?ensureStartsWith("ab", "ab!", 'i') />
<@assertEquals expected='abABcdef' actual="ABcdef"?ensureStartsWith(".b", "ab", 'i') />
<@assertEquals expected='ABcdef' actual="ABcdef"?ensureStartsWith(".b", "ab", 'ri') />
<@assertEquals expected='http://example.com' actual="example.com"?ensureStartsWith("[a-z]+://", "http://") />
<@assertEquals expected='http://example.com' actual="http://example.com"?ensureStartsWith("[a-z]+://", "http://") />
<@assertEquals expected='https://example.com' actual="https://example.com"?ensureStartsWith("[a-z]+://", "http://") />
<@assertEquals expected='http://HTTP://example.com' actual="HTTP://example.com"?ensureStartsWith("[a-z]+://", "http://") />
<@assertEquals expected='HTTP://example.com' actual="HTTP://example.com"?ensureStartsWith("[a-z]+://", "http://", "ir") />
<@assertFails message='can only have'>
    ${'x'?ensureStartsWith('x', 'x', 'x', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?ensureStartsWith()}
</@assertFails>

<@assertEquals expected='foox' actual='foo'?ensureEndsWith('x') />
<@assertEquals expected='foo' actual='foo'?ensureEndsWith('o') />
<@assertEquals expected='foo' actual='foo'?ensureEndsWith('foo') />
<@assertEquals expected='foofooo' actual='foo'?ensureEndsWith('fooo') />
<@assertEquals expected='foo' actual='foo'?ensureEndsWith('') />
<@assertEquals expected='x' actual=''?ensureEndsWith('x') />
<@assertEquals expected='' actual=''?ensureEndsWith('') />
<@assertFails message='can only have'>
    ${'x'?ensureEndsWith('x', 'x')}
</@assertFails>
<@assertFails message='argument'>
    ${'x'?ensureEndsWith()}
</@assertFails>

<@assertEquals expected='a' actual=1?lowerAbc />
<@assertEquals expected='b' actual=2?lowerAbc />
<@assertEquals expected='z' actual=26?lowerAbc />
<@assertEquals expected='aa' actual=27?lowerAbc />
<@assertEquals expected='ab' actual=28?lowerAbc />
<@assertEquals expected='cv' actual=100?lowerAbc />
<@assertFails messageRegexp='0|at least 1'>>
    ${0?lowerAbc}
</@assertFails>
<@assertFails messageRegexp='0|at least 1'>
    ${-1?lowerAbc}
</@assertFails>
<@assertFails messageRegexp='1.00001|integer'>
    ${1.00001?lowerAbc}
</@assertFails>

<@assertEquals expected='A' actual=1?upperAbc />
<@assertEquals expected='B' actual=2?upperAbc />
<@assertEquals expected='Z' actual=26?upperAbc />
<@assertEquals expected='AA' actual=27?upperAbc />
<@assertEquals expected='AB' actual=28?upperAbc />
<@assertEquals expected='CV' actual=100?upperAbc />
<@assertFails messageRegexp='0|at least 1'>>
    ${0?upperAbc}
</@assertFails>
<@assertFails messageRegexp='0|at least 1'>
    ${-1?upperAbc}
</@assertFails>
<@assertFails messageRegexp='1.00001|integer'>
    ${1.00001?upperAbc}
</@assertFails>
