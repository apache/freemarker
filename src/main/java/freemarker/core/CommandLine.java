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

package freemarker.core;

import freemarker.template.Configuration;
import freemarker.template.Version;
import freemarker.template.utility.DateUtil;

/**
 * FreeMarker command-line utility, the Main-Class of <tt>freemarker.jar</tt>.
 * Currently it just prints the version number.
 * 
 * @deprecated Will be removed (main method in a library, often classified as CWE-489 "Leftover Debug Code").
 */
public class CommandLine {
    
    public static void main(String[] args) {
        Version ver = Configuration.getVersion();
        
        System.out.println();
        System.out.print("FreeMarker version ");
        System.out.print(ver);
        
        /* If the version number doesn't already contain the build date and it's known, print it: */
        if (!ver.toString().endsWith("Z")
        		&& ver.getBuildDate() != null) {
	        System.out.print(" (built on ");
	        System.out.print(DateUtil.dateToISO8601String(
	        		ver.getBuildDate(),
	        		true, true, true, DateUtil.ACCURACY_SECONDS,
	        		DateUtil.UTC,
	        		new DateUtil.TrivialDateToISO8601CalendarFactory()));
	        System.out.print(")");
        }
        System.out.println();
        
        if (ver.isGAECompliant() != null) {
            System.out.print("Google App Engine complian variant: ");
            System.out.println(ver.isGAECompliant().booleanValue() ? "Yes" : "No");
        }
        
        System.out.println();
        System.out.println("Copyright (c) 2014 Attila Szegedi, Daniel Dekany, Jonathan Revusky.");
        System.out.println("Licensed under the Apache License, Version 2.0");
        System.out.println();
        System.out.println("For more information and for updates visit our Web site:");
        System.out.println("http://freemarker.org/");
        System.out.println();
    }
}