<@assertEquals expected='foo' actual='foo'?keep_before('x') />
<@assertEquals expected='f' actual='foo'?keep_before('o') />
<@assertEquals expected='' actual='foo'?keep_before('f') />
<@assertEquals expected='fo' actual='foobar'?keep_before('ob') />
<@assertEquals expected='foob' actual='foobar'?keep_before('ar') />
<@assertEquals expected='' actual='foobar'?keep_before('foobar') />
<@assertEquals expected='' actual='foobar'?keep_before('') />
<@assertEquals expected='' actual='foobar'?keep_before('', 'r') />
<@assertEquals expected='FOO' actual='FOO'?keep_before('o') />
<@assertEquals expected='F' actual='FOO'?keep_before('o', 'i') />
<@assertEquals expected='fo' actual='fo.o'?keep_before('.') />
<@assertEquals expected='' actual='fo.o'?keep_before('.', 'r') />
<@assertEquals expected='FOOb' actual='FOObaar'?keep_before(r'([a-z])\1', 'r') />
<@assertEquals expected='F' actual='FOObaar'?keep_before(r'([a-z])\1', 'ri') />
<@assertEquals expected='foo' actual="foo : bar"?keep_before(r"\s*:\s*", "r") />
<@assertEquals expected='foo' actual="foo:bar"?keep_before(r"\s*:\s*", "r") />
<@assertFails message='"m" flag'>
    ${'x'?keep_before('x', 'm')}
</@assertFails>
<@assertFails message='3'>
    ${'x'?keep_before('x', 'i', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?keep_before()}
</@assertFails>

<@assertEquals expected='' actual=''?keep_before_last('f') />
<@assertEquals expected='ff' actual='fff'?keep_before_last('f') />
<@assertEquals expected='' actual='foo'?keep_before_last('f') />
<@assertEquals expected='' actual='f'?keep_before_last('f') />
<@assertEquals expected='a.b' actual='a.b.txt'?keep_before_last('.') />
<@assertEquals expected='ab' actual='ab'?keep_before_last('.') />
<@assertEquals expected='a' actual='ab'?keep_before_last('.', 'r') />
<@assertEquals expected='ab' actual='ab'?keep_before_last(r'\.', 'r') />
<@assertEquals expected='af' actual='afFf'?keep_before_last('F') />
<@assertEquals expected='afF' actual='afFf'?keep_before_last('F', 'i') />
<@assertEquals expected='1a2' actual='1a2b3'?keep_before_last('[ab]', 'r') />
<@assertEquals expected='aa' actual='aaabb'?keep_before_last('[ab]{3}', 'r') />
<@assertEquals expected='aaabbx' actual='aaabbxbabe'?keep_before_last('[ab]{3}', 'r') />
<@assertEquals expected='xxxaa' actual='xxxaaayyy'?keep_before_last('a+', 'r') />
<@assertEquals expected='foobar' actual='foobar'?keep_before_last('') />
<@assertEquals expected='foobar' actual='foobar'?keep_before_last('', 'r') />
<@assertFails message='"m" flag'>
    ${'x'?keep_before_last('x', 'm')}
</@assertFails>
<@assertFails message='3'>
    ${'x'?keep_before_last('x', 'i', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?keep_before_last()}
</@assertFails>

<@assertEquals expected='' actual='foo'?keep_after('x') />
<@assertEquals expected='o' actual='foo'?keep_after('o') />
<@assertEquals expected='oo' actual='foo'?keep_after('f') />
<@assertEquals expected='ar' actual='foobar'?keep_after('ob') />
<@assertEquals expected='' actual='foobar'?keep_after('ar') />
<@assertEquals expected='' actual='foobar'?keep_after('foobar') />
<@assertEquals expected='foobar' actual='foobar'?keep_after('') />
<@assertEquals expected='foobar' actual='foobar'?keep_after('', 'r') />
<@assertEquals expected='' actual='FOO'?keep_after('o') />
<@assertEquals expected='O' actual='FOO'?keep_after('o', 'i') />
<@assertEquals expected='o' actual='fo.o'?keep_after('.') />
<@assertEquals expected='o.o' actual='fo.o'?keep_after('.', 'r') />
<@assertEquals expected='r' actual='FOObaar'?keep_after(r'([a-z])\1', 'r') />
<@assertEquals expected='baar' actual='FOObaar'?keep_after(r'([a-z])\1', 'ri') />
<@assertEquals expected='bar' actual="foo : bar"?keep_after(r"\s*:\s*", "r") />
<@assertEquals expected='bar' actual="foo:bar"?keep_after(r"\s*:\s*", "r") />
<@assertFails message='"m" flag'>
    ${'x'?keep_after('x', 'm')}
</@assertFails>
<@assertFails message='3'>
    ${'x'?keep_after('x', 'i', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?keep_after()}
</@assertFails>

<@assertEquals expected='' actual=''?keep_after_last('f') />
<@assertEquals expected='' actual='fff'?keep_after_last('f') />
<@assertEquals expected='' actual='oof'?keep_after_last('f') />
<@assertEquals expected='' actual='f'?keep_after_last('f') />
<@assertEquals expected='txt' actual='a.b.txt'?keep_after_last('.') />
<@assertEquals expected='' actual='ab'?keep_after_last('.') />
<@assertEquals expected='' actual='ab'?keep_after_last('.', 'r') />
<@assertEquals expected='' actual='ab'?keep_after_last(r'\.', 'r') />
<@assertEquals expected='fa' actual='fFfa'?keep_after_last('F') />
<@assertEquals expected='a' actual='fFfa'?keep_after_last('F', 'i') />
<@assertEquals expected='3' actual='1a2b3'?keep_after_last('[ab]', 'r') />
<@assertEquals expected='' actual='aaabb'?keep_after_last('[ab]{3}', 'r') />
<@assertEquals expected='x' actual='aaabbx'?keep_after_last('[ab]{3}', 'r') />
<@assertEquals expected='e' actual='aaabbxbabe'?keep_after_last('[ab]{3}', 'r') />
<@assertEquals expected='12345' actual='aaabb12345'?keep_after_last('[ab]{3}', 'r') />
<@assertEquals expected='yyy' actual='xxxaaayyy'?keep_after_last('a+', 'r') />
<@assertEquals expected='' actual='foobar'?keep_after_last('') />
<@assertEquals expected='' actual='foobar'?keep_after_last('', 'r') />
<@assertFails message='"m" flag'>
    ${'x'?keep_after_last('x', 'm')}
</@assertFails>
<@assertFails message='3'>
    ${'x'?keep_after_last('x', 'i', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?keep_after_last()}
</@assertFails>

<@assertEquals expected='foo' actual='foo'?remove_beginning('x') />
<@assertEquals expected='foo' actual='foo'?remove_beginning('o') />
<@assertEquals expected='foo' actual='foo'?remove_beginning('fooo') />
<@assertEquals expected='oo' actual='foo'?remove_beginning('f') />
<@assertEquals expected='o' actual='foo'?remove_beginning('fo') />
<@assertEquals expected='' actual='foo'?remove_beginning('foo') />
<@assertEquals expected='foo' actual='foo'?remove_beginning('') />
<@assertFails message='2'>
    ${'x'?remove_beginning('x', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?remove_beginning()}
</@assertFails>

<@assertEquals expected='bar' actual='bar'?remove_ending('x') />
<@assertEquals expected='bar' actual='bar'?remove_ending('a') />
<@assertEquals expected='bar' actual='bar'?remove_ending('barr') />
<@assertEquals expected='ba' actual='bar'?remove_ending('r') />
<@assertEquals expected='b' actual='bar'?remove_ending('ar') />
<@assertEquals expected='' actual='bar'?remove_ending('bar') />
<@assertEquals expected='bar' actual='bar'?remove_ending('') />
<@assertFails message='2'>
    ${'x'?remove_ending('x', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?remove_ending()}
</@assertFails>

<@assertEquals expected='xfoo' actual='foo'?ensure_starts_with('x') />
<@assertEquals expected='foo' actual='foo'?ensure_starts_with('f') />
<@assertEquals expected='foo' actual='foo'?ensure_starts_with('foo') />
<@assertEquals expected='fooofoo' actual='foo'?ensure_starts_with('fooo') />
<@assertEquals expected='foo' actual='foo'?ensure_starts_with('') />
<@assertEquals expected='x' actual=''?ensure_starts_with('x') />
<@assertEquals expected='' actual=''?ensure_starts_with('') />
<@assertEquals expected='bacdef' actual="bacdef"?ensure_starts_with("[ab]{2}", "ab") />
<@assertEquals expected='bacdef' actual="bacdef"?ensure_starts_with("^[ab]{2}", "ab") />
<@assertEquals expected='abcacdef' actual="cacdef"?ensure_starts_with("[ab]{2}", "ab") />
<@assertEquals expected='abcacdef' actual="cacdef"?ensure_starts_with("^[ab]{2}", "ab") />
<@assertEquals expected='ab!cdef' actual="cdef"?ensure_starts_with("ab", "ab!") />
<@assertEquals expected='ab!ABcdef' actual="ABcdef"?ensure_starts_with("ab", "ab!") />
<@assertEquals expected='ABcdef' actual="ABcdef"?ensure_starts_with("ab", "ab!", 'i') />
<@assertEquals expected='abABcdef' actual="ABcdef"?ensure_starts_with(".b", "ab", 'i') />
<@assertEquals expected='ABcdef' actual="ABcdef"?ensure_starts_with(".b", "ab", 'ri') />
<@assertEquals expected='http://example.com' actual="example.com"?ensure_starts_with("[a-z]+://", "http://") />
<@assertEquals expected='http://example.com' actual="http://example.com"?ensure_starts_with("[a-z]+://", "http://") />
<@assertEquals expected='https://example.com' actual="https://example.com"?ensure_starts_with("[a-z]+://", "http://") />
<@assertEquals expected='http://HTTP://example.com' actual="HTTP://example.com"?ensure_starts_with("[a-z]+://", "http://") />
<@assertEquals expected='HTTP://example.com' actual="HTTP://example.com"?ensure_starts_with("[a-z]+://", "http://", "ir") />
<@assertFails message='4'>
    ${'x'?ensure_starts_with('x', 'x', 'x', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?ensure_starts_with()}
</@assertFails>

<@assertEquals expected='foox' actual='foo'?ensure_ends_with('x') />
<@assertEquals expected='foo' actual='foo'?ensure_ends_with('o') />
<@assertEquals expected='foo' actual='foo'?ensure_ends_with('foo') />
<@assertEquals expected='foofooo' actual='foo'?ensure_ends_with('fooo') />
<@assertEquals expected='foo' actual='foo'?ensure_ends_with('') />
<@assertEquals expected='x' actual=''?ensure_ends_with('x') />
<@assertEquals expected='' actual=''?ensure_ends_with('') />
<@assertFails message='2'>
    ${'x'?ensure_ends_with('x', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?ensure_ends_with()}
</@assertFails>

<@assertEquals expected='a' actual=1?lower_abc />
<@assertEquals expected='b' actual=2?lower_abc />
<@assertEquals expected='z' actual=26?lower_abc />
<@assertEquals expected='aa' actual=27?lower_abc />
<@assertEquals expected='ab' actual=28?lower_abc />
<@assertEquals expected='cv' actual=100?lower_abc />
<@assertFails messageRegexp='0|at least 1']>
    ${0?lower_abc}
</@assertFails>
<@assertFails messageRegexp='0|at least 1'>
    ${-1?lower_abc}
</@assertFails>
<@assertFails messageRegexp='1.00001|integer'>
    ${1.00001?lower_abc}
</@assertFails>

<@assertEquals expected='A' actual=1?upper_abc />
<@assertEquals expected='B' actual=2?upper_abc />
<@assertEquals expected='Z' actual=26?upper_abc />
<@assertEquals expected='AA' actual=27?upper_abc />
<@assertEquals expected='AB' actual=28?upper_abc />
<@assertEquals expected='CV' actual=100?upper_abc />
<@assertFails messageRegexp='0|at least 1']>
    ${0?upper_abc}
</@assertFails>
<@assertFails messageRegexp='0|at least 1'>
    ${-1?upper_abc}
</@assertFails>
<@assertFails messageRegexp='1.00001|integer'>
    ${1.00001?upper_abc}
</@assertFails>
