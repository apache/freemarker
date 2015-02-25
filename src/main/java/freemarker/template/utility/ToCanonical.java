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

package freemarker.template.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Read in a template and convert it to a canonical format.
 * 
 * @deprecated Will be removed (main method in a library, often classified as CWE-489 "Leftover Debug Code").
 */
public class ToCanonical {

    static Configuration config = Configuration.getDefaultConfiguration();

    /**
     * @deprecated Will be removed (main method in a library, often classified as CWE-489 "Leftover Debug Code").
     */
    static public void main(String[] args) {
        config.setWhitespaceStripping(false);
        if (args.length == 0) {
            usage();
        }
        for (int i=0; i<args.length; i++) {
            File f = new File(args[i]);
            if (!f.exists()) {
                System.err.println("File " + f + " doesn't exist.");
            }
            try {
                convertFile(f);
            } catch (Exception e) {
                System.err.println("Error converting file: " + f);
                e.printStackTrace();
            }
        }
    }

    static void convertFile(File f) throws IOException {
        File fullPath = f.getAbsoluteFile();
        File dir = fullPath.getParentFile();
        String filename = fullPath.getName();
        File convertedFile = new File(dir, filename + ".canonical");
        config.setDirectoryForTemplateLoading(dir);
        Template template = config.getTemplate(filename);
        FileWriter output = new FileWriter(convertedFile);
        try {
            template.dump(output);
        } finally {
            output.close();
        }
    }

    static void usage() {
        System.err.println("Usage: java freemarker.template.utility.ToCanonical <filename(s)>");
    }
}
