/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package freemarker.ext.dom;

import freemarker.core.Environment;
import freemarker.template.Template;

/**
 * For internal use only; don't depend on this, there's no backward compatibility guarantee at all!
 * This class is to work around the lack of module system in Java, i.e., so that other FreeMarker packages can
 * access things inside this package that users shouldn't. 
 */
final class DomStringUtil {

    private DomStringUtil() {
        // Not meant to be instantiated
    }

    static boolean isXMLNameLike(String name) {
        return isXMLNameLike(name, 0);
    }
    
    /**
     * Check if the name looks like an XML element name.
     * 
     * @param firstCharIdx The index of the character in the string parameter that we treat as the beginning of the
     *      string to check. This is to spare substringing that has become more expensive in Java 7.  
     * 
     * @return whether the name is a valid XML element name. (This routine might only be 99% accurate. REVISIT)
     */
    static boolean isXMLNameLike(String name, int firstCharIdx) {
        int ln = name.length();
        for (int i = firstCharIdx; i < ln; i++) {
            char c = name.charAt(i);
            if (i == firstCharIdx && (c == '-' || c == '.' || Character.isDigit(c))) {
                return false;
            }
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.') {
                if (c == ':') {
                    if (i + 1 < ln && name.charAt(i + 1) == ':') {
                        // "::" is used in XPath
                        return false;
                    }
                    // We don't return here, as a lonely ":" is allowed.
                } else {
                    return false;
                }
            }
        }
        return true;
    }    

    /**
     * @return whether the qname matches the combination of nodeName, nsURI, and environment prefix settings. 
     */
    static boolean matchesName(String qname, String nodeName, String nsURI, Environment env) {
        String defaultNS = env.getDefaultNS();
        if ((defaultNS != null) && defaultNS.equals(nsURI)) {
            return qname.equals(nodeName) 
               || qname.equals(Template.DEFAULT_NAMESPACE_PREFIX + ":" + nodeName); 
        }
        if ("".equals(nsURI)) {
            if (defaultNS != null) {
                return qname.equals(Template.NO_NS_PREFIX + ":" + nodeName);
            } else {
                return qname.equals(nodeName) || qname.equals(Template.NO_NS_PREFIX + ":" + nodeName);
            }
        }
        String prefix = env.getPrefixForNamespace(nsURI);
        if (prefix == null) {
            return false; // Is this the right thing here???
        }
        return qname.equals(prefix + ":" + nodeName);
    }
    
}
