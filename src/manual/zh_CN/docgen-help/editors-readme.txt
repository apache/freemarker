Guide to FreeMarker Manual for Editors
======================================

Non-technical
-------------

- The Template Author's Guide is for Web designers. Assume that a
  designer is not a programmer, (s)he doesn't even know what is Java.
  Forget that FM is implemented in Java when you edit the Template
  Author's Guide. Try to avoid technical writing.

- In the Guide chapters, be careful not to mention things that were
  not explained earlier. The Guide chapters should be understandable
  if you read them continuously.

- If you add a new topic or term, don't forget to add it to the Index.
  Also, consider adding entries for it to the Glossary.

- Don't use too sophisticated English. Use basic words and grammar.


Technical
---------

- For the editing use XXE (XMLmind XML Editor), with its default XML
  *source* formatting settings (identation, max line length and like).
  You should install the "DocBook 5 for Freemarker" addon, which you can
  find inside the "docgen" top-level SVN module.

- The HTML is generated with Docgen (docgen.jar), which will check some
  of the rules described here. To invoke it, issue "ant manual" from
  the root of the "freemarker" module. (Note: you may need to check out
  and build "docgen" first.)

- Understand all document conventions in the Preface chapter. Note that
  all "programlisting"-s should have a "role" attribute with a value that
  is either: "template", "dataModel", "output", "metaTemplate" or
  "unspecified". (If you miss this, the XXE addon will show the
  "programlisting" in red.)

- Verbatim content in flow text:

  * In flow text, all data object names, class names, FTL fragments,
    HTML fragments, and all other verbatim content is inside "literal"
    element.

  * Use replaceable element inside literal element for replaceable
    parts and meta-variables like:
    <literal&lt;if <replaceable>condition</replaceable>></literal>
    <literal><replaceable>templateDir</replaceable>/copyright.ftl</literal>

- Hierarchy:

  * The hierarchy should look like:

      book -> part -> chapter -> section -> section -> section -> section

    where the "part" and the "section"-s are optional.
    Instead of chapter you may have "preface" or "appendix".

  * Don't use "sect1", "sect2", etc. Instead nest "section"-s into each other,
    but not deeper than 3 levels.

  * Use "simplesect" if you want to divide up something visually, but
    you don't want those sections to appear in the ToC, or go into their own
    HTML page. "simplesect"-s can appear under all "section" nesting
    levels, and they always look the same regardless of the "section"
    nesting levels.

- Lists:

  * When you have list where the list items are short (a few words),
    you should give spacing="compact" to the "itemizedlist" or
    "orderedlist" element.

  * Don't putting listings inside "para"-s. Put them between "para"-s instead.

- Xrefs, id-s, links:

  * id-s of parts, chapters, sections and similar elements must
    contain US-ASCII lower case letters, US-ASCII numbers, and
    underscore only. id-s of parts and chapters are used as the
    filenames of HTML-s generated for that block.
    When you find out the id, deduce it from the position in the ToC
    hierarchy. The underscore is used as the separator between the path
    steps.

  * All other id-s must use prefix:
    - example: E.g.: id="example.foreach"
    - ref: Reference information...
      * directive: about a directive. E.g.: "ref.directive.foreach"
      * builtin
    - gloss: Term in the Glossary
    - topic: The recommended point of document in a certain topic
      * designer: for designers.
          E.g.: id="topic.designer.methodDataObject"
      * programmer: for programmers
      * or omit the secondary category if it is for everybody
    - misc: Anything doesn't fit in the above categories

  * When you refer to a part, chapter or section, often you should use
    xref, not link. The xreflabel attribute of the link-end should not be set;
    then it's deduced from the titles.

- The "book" element must have this attribute: conformance="docgen"
