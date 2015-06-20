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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.utility.ClassUtil;
import freemarker.template.utility.StringUtil;

/**
 * Static methods and command-line tool for printing the AST of a template. 
 */
public class ASTPrinter {

    private final Configuration cfg;
    private int successfulCounter;
    private int failedCounter;
    
    static public void main(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            System.exit(-1);
        }
        
        ASTPrinter astp = new ASTPrinter(); 
        if (args[0].equalsIgnoreCase("-r")) {
            astp.mainRecursive(args);
        } else {
            astp.mainSingleTemplate(args);
        }
    }
    
    private ASTPrinter() {
        cfg = new Configuration();
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_20);
    }
    
    private void mainSingleTemplate(String[] args) throws IOException, FileNotFoundException {
        final String templateFileName;
        final String templateContent;
        if (args[0].startsWith("ftl:")) {
            templateFileName = null;
            templateContent = args[0];
        } else {
            templateFileName = args[0];
            templateContent = null;
        }
        
        Template t = new Template(
                templateFileName,
                templateFileName == null ? new StringReader(templateContent) : new FileReader(templateFileName),
                cfg);
        
        p(getASTAsString(t));
    }

    private void mainRecursive(String[] args) throws IOException {
        if (args.length != 4) {
            p("Number of arguments must be 4, but was: " + args.length);
            usage();
            System.exit(-1);
        }
        
        final String srcDirPath = args[1].trim();
        File srcDir = new File(srcDirPath);
        if (!srcDir.isDirectory()) {
            p("This should be an existing directory: " + srcDirPath);
            System.exit(-1);
        }
        
        Pattern fnPattern;
        try {
            fnPattern = Pattern.compile(args[2]);
        } catch (PatternSyntaxException e) {
            p(StringUtil.jQuote(args[2]) + " is not a valid regular expression");
            System.exit(-1);
            return;
        }
        
        final String dstDirPath = args[3].trim();
        File dstDir = new File(dstDirPath);
        if (!dstDir.isDirectory()) {
            p("This should be an existing directory: " + dstDirPath);
            System.exit(-1);
        }
        
        long startTime = System.currentTimeMillis();
        recurse(srcDir, fnPattern, dstDir);
        long endTime = System.currentTimeMillis();
        
        p("Templates successfully processed " + successfulCounter + ", failed " + failedCounter
                + ". Time taken: " + (endTime - startTime) / 1000.0 + " s");
    }
    
    private void recurse(File srcDir, Pattern fnPattern, File dstDir) throws IOException {
        File[] files = srcDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                recurse(file, fnPattern, new File(dstDir, file.getName()));
            } else {
                if (fnPattern.matcher(file.getName()).matches()) {
                    File dstFile = new File(dstDir, file.getName());
                    String res;
                    try {
                        Template t = new Template(file.getPath().replace('\\', '/'), loadIntoString(file), cfg);
                        res = getASTAsString(t);
                        successfulCounter++;
                    } catch (ParseException e) {
                        res = "<<<FAILED>>>\n" + e.getMessage();
                        failedCounter++;
                        p("");
                        p("-------------------------failed-------------------------");
                        p("Error message was saved into: " + dstFile.getAbsolutePath());
                        p("");
                        p(e.getMessage());
                    }
                    save(res, dstFile);
                }
            }
        }
    }

    private String loadIntoString(File file) throws IOException {
        long ln = file.length();
        if (ln < 0) {
            throw new IOException("Failed to get the length of " + file);
        }
        byte[] buffer = new byte[(int) ln];
        InputStream in = new FileInputStream(file);
        try {
            int offset = 0;
            int bytesRead;
            while (offset < buffer.length) {
                bytesRead = in.read(buffer, offset, buffer.length - offset);
                if (bytesRead == -1) {
                    throw new IOException("Unexpected end of file: " + file);
                }
                offset += bytesRead;
            }
        } finally {
            in.close();
        }
        
        try {
            return decode(buffer, Charset.forName("UTF-8"));
        } catch (CharacterCodingException e) {
            return decode(buffer, Charset.forName("ISO-8859-1"));
        }
    }

    private String decode(byte[] buffer, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(buffer)).toString();
    }

    private void save(String astStr, File file) throws IOException {
        File parentDir = file.getParentFile();
        if (!parentDir.isDirectory() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create parent directory: " + parentDir);
        }
        
        Writer w = new BufferedWriter(new FileWriter(file));
        try {
            w.write(astStr);
        } finally {
            w.close();
        }
    }

    private static void usage() {
        p("Prints template Abstract Syntax Tree (AST) as plain text.");
        p("Usage:");
        p("    java freemarker.core.PrintAST <templateFile>");
        p("    java freemarker.core.PrintAST ftl:<templateSource>");
        p("    java freemarker.core.PrintAST -r <src-directory> <regexp> <dst-directory>");
    }

    private static final String INDENTATION = "    ";

    public static String getASTAsString(String ftl) throws IOException {
        return getASTAsString(ftl, (Options) null);
    }
    
    public static String getASTAsString(String ftl, Options opts) throws IOException {
        return getASTAsString(null, ftl, opts);
    }

    public static String getASTAsString(String templateName, String ftl) throws IOException {
        return getASTAsString(templateName, ftl, null);
    }
    
    public static String getASTAsString(String templateName, String ftl, Options opts) throws IOException {
        Configuration cfg = new Configuration();
        Template t = new Template(templateName, ftl, cfg);
        return getASTAsString(t, opts);
    }

    public static String getASTAsString(Template t) throws IOException {
        return getASTAsString(t, null);
    }

    public static String getASTAsString(Template t, Options opts) throws IOException {
        validateAST(t);
        
        StringWriter out = new StringWriter();
        printNode(t.getRootTreeNode(), "", null, opts != null ? opts : Options.DEFAULT_INSTANCE, out);
        return out.toString();
    }
    
    public static void validateAST(Template t) throws InvalidASTException {
        final TemplateElement node = t.getRootTreeNode();
        if (node.getParentElement() != null) {
            throw new InvalidASTException("Root node parent must be null."
                    + "\nRoot node: " + node.dump(false)
                    + "\nParent"
                    + ": " + node.getParentElement().getClass() + ", " + node.getParentElement().dump(false));
        }
        validateAST(node);
    }

    private static void validateAST(TemplateElement te) {
        int ln = te.getRegulatedChildCount();
        for (int i = 0; i < ln; i++) {
            TemplateElement child = te.getRegulatedChild(i);
            if (child.getParentElement() != te) {
                throw new InvalidASTException("Wrong parent node."
                        + "\nNode: " + child.dump(false)
                        + "\nExpected parent: " + te.dump(false)
                        + "\nActual parent: " + child.getParentElement().dump(false));
            }
            if (child.getIndex() != i) {
                throw new InvalidASTException("Wrong node index."
                        + "\nNode: " + child.dump(false)
                        + "\nExpected index: " + i
                        + "\nActual index: " + child.getIndex());
            }
        }
        if (te instanceof MixedContent && te.getRegulatedChildCount() < 2) {
            throw new InvalidASTException("Mixed content with child count less than 2 should removed by optimizatoin, "
                    + "but found one with " + te.getRegulatedChildCount() + " child(ren).");
        }
        if (te.getRegulatedChildCount() != 0 && te.getNestedBlock() != null) {
            throw new InvalidASTException("Can't have both nestedBlock and regulatedChildren."
                    + "\nNode: " + te.dump(false));
        }
    }

    private static void printNode(Object node, String ind, ParameterRole paramRole, Options opts, Writer out) throws IOException {
        if (node instanceof TemplateObject) {
            TemplateObject tObj = (TemplateObject) node;

            printNodeLineStart(paramRole, ind, out);
            out.write(tObj.getNodeTypeSymbol());
            printNodeLineEnd(node, out, opts);
            
            if (opts.getShowConstantValue() && node instanceof Expression) {
                TemplateModel tm = ((Expression) node).constantValue;
                if (tm != null) {
                    out.write(INDENTATION);
                    out.write(ind);
                    out.write("= const ");
                    out.write(ClassUtil.getFTLTypeDescription(tm));
                    out.write(' ');
                    out.write(tm.toString());
                    out.write('\n');
                }
            }
            
            int paramCnt = tObj.getParameterCount();
            for (int i = 0; i < paramCnt; i++) {
                ParameterRole role = tObj.getParameterRole(i);
                if (role == null) throw new NullPointerException("parameter role");
                Object value = tObj.getParameterValue(i);
                printNode(value, ind + INDENTATION, role, opts, out);
            }
            if (tObj instanceof TemplateElement) {
                Enumeration enu = ((TemplateElement) tObj).children();
                while (enu.hasMoreElements()) {
                    printNode(enu.nextElement(), INDENTATION + ind, null, opts, out);
                }
            }
        } else {
            printNodeLineStart(paramRole, ind, out);
            out.write(StringUtil.jQuote(node));
            printNodeLineEnd(node, out, opts);
        }
    }

    protected static void printNodeLineEnd(Object node, Writer out, Options opts) throws IOException {
        boolean commentStared = false;
        if (opts.getShowJavaClass()) {
            out.write("  // ");
            commentStared = true;
            out.write(ClassUtil.getShortClassNameOfObject(node, true));
        }
        if (opts.getShowLocation() && node instanceof TemplateObject) {
            if (!commentStared) {
                out.write("  // ");
                commentStared = true;
            } else {
                out.write("; ");
            }
            TemplateObject tObj = (TemplateObject) node;
            out.write("Location " + tObj.beginLine + ":" + tObj.beginColumn + "-" + tObj.endLine + ":" + tObj.endColumn);
        }
        out.write('\n');
    }

    private static void printNodeLineStart(ParameterRole paramRole, String ind, Writer out) throws IOException {
        out.write(ind);
        if (paramRole != null) {
            out.write("- ");
            out.write(paramRole.toString());
            out.write(": ");
        }
    }
    
    public static class Options {
        
        private final static Options DEFAULT_INSTANCE = new Options(); 
        
        private boolean showJavaClass = true;
        private boolean showConstantValue = false;
        private boolean showLocation = false;
        
        public boolean getShowJavaClass() {
            return showJavaClass;
        }
        
        public void setShowJavaClass(boolean showJavaClass) {
            this.showJavaClass = showJavaClass;
        }
        
        public boolean getShowConstantValue() {
            return showConstantValue;
        }
        
        public void setShowConstantValue(boolean showConstantValue) {
            this.showConstantValue = showConstantValue;
        }

        public boolean getShowLocation() {
            return showLocation;
        }

        public void setShowLocation(boolean showLocation) {
            this.showLocation = showLocation;
        }
        
    }
    
    private static void p(Object obj) {
        System.out.println(obj);
    }

    public static class InvalidASTException extends RuntimeException {

        public InvalidASTException(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidASTException(String message) {
            super(message);
        }
        
    }
}
