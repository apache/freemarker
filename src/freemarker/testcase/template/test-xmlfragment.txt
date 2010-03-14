<#ftl ns_prefixes = {"n" : "http://x"}>
${node?node_name} = b
${node?root?node_name} = @document
${node['/']?node_name} = @document

${node['n:c']} = C<>&"']]>

${node?root.@@markup}