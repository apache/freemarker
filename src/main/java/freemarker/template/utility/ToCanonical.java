/*
 * Copyright (c) 2003 The Visigoth Software Society. All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Visigoth Software Society (http://www.visigoths.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. Neither the name "FreeMarker", "Visigoth", nor any of the names of the 
 *    project contributors may be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact visigoths@visigoths.org.
 *
 * 5. Products derived from this software may not be called "FreeMarker" or "Visigoth"
 *    nor may "FreeMarker" or "Visigoth" appear in their names
 *    without prior written permission of the Visigoth Software Society.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE VISIGOTH SOFTWARE SOCIETY OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Visigoth Software Society. For more
 * information on the Visigoth Software Society, please see
 * http://www.visigoths.org/
 */

package freemarker.template.utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Read in a template and convert it to a canonical format.
 */

public class ToCanonical {

    static Configuration config = Configuration.getDefaultConfiguration();

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
