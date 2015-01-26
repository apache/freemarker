/*
 * Copyright 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freemarker.log;

/**
 * Don't use this class; it's only public to work around Google App Engine Java
 * compliance issues. FreeMarker developers only: treat this class as package-visible.
 */
public class _Log4jOverSLF4JTester {

    private static final String MDC_KEY = _Log4jOverSLF4JTester.class.getName();

    /**
     * Returns if Log4j-over-SLF4J is actually working. Sometimes the API classes are present, but there's no SLF4J
     * implementation around.
     */
    public static final boolean test() {
        org.apache.log4j.MDC.put(MDC_KEY, "");
        try {
            return org.slf4j.MDC.get(MDC_KEY) != null;
        } finally {
            org.apache.log4j.MDC.remove(MDC_KEY);
        }
    }

}
