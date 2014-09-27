<@assertEquals expected='foo' actual='foo'?keep_until('x') />
<@assertEquals expected='f' actual='foo'?keep_until('o') />
<@assertEquals expected='' actual='foo'?keep_until('f') />
<@assertEquals expected='fo' actual='foobar'?keep_until('ob') />
<@assertEquals expected='foob' actual='foobar'?keep_until('ar') />
<@assertEquals expected='' actual='foobar'?keep_until('foobar') />
<@assertEquals expected='' actual='foobar'?keep_until('') />
<@assertEquals expected='' actual='foobar'?keep_until('', 'r') />
<@assertEquals expected='FOO' actual='FOO'?keep_until('o') />
<@assertEquals expected='F' actual='FOO'?keep_until('o', 'i') />
<@assertEquals expected='fo' actual='fo.o'?keep_until('.') />
<@assertEquals expected='' actual='fo.o'?keep_until('.', 'r') />
<@assertEquals expected='FOOb' actual='FOObaar'?keep_until(r'([a-z])\1', 'r') />
<@assertEquals expected='F' actual='FOObaar'?keep_until(r'([a-z])\1', 'ri') />
<@assertFails message="'r' flag">
    ${'x'?keep_until('x', 'if')}
</@assertFails>
<@assertFails message='3'>
    ${'x'?keep_until('x', 'i', 'x')}
</@assertFails>
<@assertFails message='none'>
    ${'x'?keep_until()}
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
<@assertFails message='2'>
    ${'x'?ensure_starts_with('x', 'x')}
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
