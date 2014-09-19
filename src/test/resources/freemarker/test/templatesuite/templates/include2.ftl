<#include "include2" + "-included.ftl">
<#assign s = "de">
<#include "inclu" + s + "2-included.ftl">

<#assign bTrue=true>
<#assign sY='y'>
<#include "include2-included.ftl">
<#include "include2-included.ftl" parse=true>
<#include "include2-included.ftl" parse=bTrue>
<#include "include2-included.ftl" parse='y'>
<#include "include2-included.ftl" parse=sY>

<#assign bFalse=false>
<#assign sN='n'>
<#include "include2-included.ftl" parse=false>
<#include "include2-included.ftl" parse=bFalse>
<#include "include2-included.ftl" parse='n'>
<#include "include2-included.ftl" parse=sN>

<#assign sEncoding="ISO-8859-1">
<#include "include2-included.ftl" encoding="ISO-8859-1">
<#include "include2-included.ftl" encoding=sEncoding>
<#include "include2-included-encoding.ftl" encoding="ISO-8859-1">
<#include "include2-included-encoding.ftl" encoding=sEncoding>

<#include "include2-included.ftl" ignore_missing=true>
<#include "include2-included.ftl" ignore_missing=bTrue>
<#include "include2-included.ftl" ignore_missing=false>
<#include "include2-included.ftl" ignore_missing=bFalse>

<@assertFails message="not found"><#include "missing.ftl"></@>
[<#include "missing.ftl" ignore_missing=true>]
[<#include "missing.ftl" ignore_missing=bTrue>]
