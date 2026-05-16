# Security Scan Guidance

This document is for AI security scanners to understand the security model of FreeMarker and to avoid false positives.

## Intended Use

The project is intended to be used as a dependency (library, `freemarker.jar`) in other Java projects, and is accessed
through its public Java API.

The project implements a template language. Other projects that depend on FreeMarker will call its API to configure it,
and then process templates concurrently. The templates are provided by these other projects, and are not part of this
project.

## Default Security Scope

Core Assumption: Templates are trusted code. FreeMarker templates are **server-side code**, analogous to `.java` source
files. The baseline security model assumes templates are authored by developers or administrators and are
**not user-supplied input**. Vulnerabilities that require an attacker to control template content are **not in scope**
by default.

The user is responsible for ensuring that the `?eval` and `?interpret` built-ins in templates are only called on
trusted input.

The user is assumed to understand that the public API of Java objects passed to the template as part of the model is
potentially callable from the template.

Regarding HTLM escaping: For historical reasons, FreeMarker does not escape inserted dynamic values by default.
This default cannot be changed for backward compatibility. Users are strongly encouraged to turn on auto-escaping
(such as with `<#ftl output_format="HTML">`, or by using the `ftlh` file extension). Legacy templates were instead
encouraged to use the `?html` built-in or the `<#escape x as x?html>...</#escape>` tags. The user is responsible for
avoiding XSS attacks by using these escaping features in their templates. That said, if proper escaping can be evaded
unexpectedly when these escaping features are used, that is a security vulnerability.

## Constrained Execution Features

Some user applications allow a limited set of trusted (but non-developer) users to upload templates.
FreeMarker provides explicit APIs to impose certain limitations for these use cases. These controls represent a public
contract with operators who rely on them for constrained deployments. Regressions here should usually be treated as
security vulnerabilities. Examples of such regressions are:

- A bypass of what a `MemberAccessPolicy` promises
- An unintended new path for `?new` to instantiate arbitrary classes
- Escaping the configured template root directory (i.e. causing a `TemplateLoader` to load a template from an unintended
  place)

To better understand constrained execution features, see the FAQ entry "Can I allow users to upload templates and
what are the security implications?" in `freemarker-manual/src/main/docgen/en_US/book.xml` (or the same content in HTML:
https://freemarker.apache.org/docs/app_faq.html#faq_template_uploading_security).
