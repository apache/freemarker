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

package freemarker.ext.dom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.StringTokenizer;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * A class that contains a main() method for command-line invocation of a FreeMarker XML transformation.
 * 
 * @deprecated Will be removed (main method in a library, often classified as CWE-489 "Leftover Debug Code").
 */
public class Transform {

    private File inputFile, ftlFile, outputFile;
    private String encoding;
    private Locale locale;
    private Configuration cfg;

    /**
     * @deprecated Will be removed (main method in a library, often classified as CWE-489 "Leftover Debug Code").
     */
    static public void main(String[] args) {
        try {
            Transform proc = transformFromArgs(args);
            proc.transform();
        } catch (IllegalArgumentException iae) {
            System.err.println(iae.getMessage());
            usage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param inputFile
     *            The file from which to read the XML input
     * @param ftlFile
     *            The file containing the template
     * @param outputFile
     *            The file to which to output. If this is null, we use stdout.
     * @param locale
     *            The locale to use. If this is null, we use the platform default.
     * @param encoding
     *            The character encoding to use for output, if this is null, we use the platform default
     */
    Transform(File inputFile, File ftlFile, File outputFile, Locale locale, String encoding) throws IOException {
        if (encoding == null) {
            encoding = System.getProperty("file.encoding");
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        this.encoding = encoding;
        this.locale = locale;
        this.inputFile = inputFile;
        this.ftlFile = ftlFile;
        this.outputFile = outputFile;
        File ftlDirectory = ftlFile.getAbsoluteFile().getParentFile();
        cfg = new Configuration();
        cfg.setDirectoryForTemplateLoading(ftlDirectory);
    }

    /**
     * Performs the transformation.
     */
    void transform() throws Exception {
        String templateName = ftlFile.getName();
        Template template = cfg.getTemplate(templateName, locale);
        NodeModel rootNode = NodeModel.parse(inputFile);
        OutputStream outputStream = System.out;
        if (outputFile != null) {
            outputStream = new FileOutputStream(outputFile);
        }
        Writer outputWriter = new OutputStreamWriter(outputStream, encoding);
        try {
            template.process(null, outputWriter, null, rootNode);
        } finally {
            if (outputFile != null)
                outputWriter.close();
        }
    }

    static Transform transformFromArgs(String[] args) throws IOException {
        int i = 0;
        String input = null, output = null, ftl = null, loc = null, enc = null;
        while (i < args.length) {
            String dashArg = args[i++];
            if (i >= args.length) {
                throw new IllegalArgumentException("");
            }
            String arg = args[i++];
            if (dashArg.equals("-in")) {
                if (input != null) {
                    throw new IllegalArgumentException("The input file should only be specified once");
                }
                input = arg;
            } else if (dashArg.equals("-ftl")) {
                if (ftl != null) {
                    throw new IllegalArgumentException("The ftl file should only be specified once");
                }
                ftl = arg;
            } else if (dashArg.equals("-out")) {
                if (output != null) {
                    throw new IllegalArgumentException("The output file should only be specified once");
                }
                output = arg;
            } else if (dashArg.equals("-locale")) {
                if (loc != null) {
                    throw new IllegalArgumentException("The locale should only be specified once");
                }
                loc = arg;
            } else if (dashArg.equals("-encoding")) {
                if (enc != null) {
                    throw new IllegalArgumentException("The encoding should only be specified once");
                }
                enc = arg;
            } else {
                throw new IllegalArgumentException("Unknown input argument: " + dashArg);
            }
        }
        if (input == null) {
            throw new IllegalArgumentException("No input file specified.");
        }
        if (ftl == null) {
            throw new IllegalArgumentException("No ftl file specified.");
        }
        File inputFile = new File(input).getAbsoluteFile();
        File ftlFile = new File(ftl).getAbsoluteFile();
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input file does not exist: " + input);
        }
        if (!ftlFile.exists()) {
            throw new IllegalArgumentException("FTL file does not exist: " + ftl);
        }
        if (!inputFile.isFile() || !inputFile.canRead()) {
            throw new IllegalArgumentException("Input file must be a readable file: " + input);
        }
        if (!ftlFile.isFile() || !ftlFile.canRead()) {
            throw new IllegalArgumentException("FTL file must be a readable file: " + ftl);
        }
        File outputFile = null;
        if (output != null) {
            outputFile = new File(output).getAbsoluteFile();
            File outputDirectory = outputFile.getParentFile();
            if (!outputDirectory.exists() || !outputDirectory.canWrite()) {
                throw new IllegalArgumentException("The output directory must exist and be writable: "
                        + outputDirectory);
            }
        }
        Locale locale = Locale.getDefault();
        if (loc != null) {
            locale = localeFromString(loc);
        }
        return new Transform(inputFile, ftlFile, outputFile, locale, enc);
    }

    static Locale localeFromString(String ls) {
        if (ls == null) ls = "";
        String lang = "", country = "", variant = "";
        StringTokenizer st = new StringTokenizer(ls, "_-,");
        if (st.hasMoreTokens()) {
            lang = st.nextToken();
            if (st.hasMoreTokens()) {
                country = st.nextToken();
                if (st.hasMoreTokens()) {
                    variant = st.nextToken();
                }
            }
            return new Locale(lang, country, variant);
        } else {
            return Locale.getDefault();
        }
    }

    static void usage() {
        System.err
                .println("Usage: java freemarker.ext.dom.Transform -in <xmlfile> -ftl <ftlfile> [-out <outfile>] [-locale <locale>] [-encoding <encoding>]");
        // Security: prevents shutting down the container from a template:
        if (Environment.getCurrentEnvironment() == null) {
            System.exit(-1);
        }
    }
}
