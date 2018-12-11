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

package org.apache.freemarker.core;

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
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.util.TemplateLanguageUtils;
import org.apache.freemarker.core.util._ClassUtils;
import org.apache.freemarker.core.util._StringUtils;
import org.apache.freemarker.test.TestConfigurationBuilder;

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
        cfg = new TestConfigurationBuilder(Configuration.VERSION_3_0_0).build();
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
            p(_StringUtils.jQuote(args[2]) + " is not a valid regular expression");
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
        if (files == null) {
            throw new IOException("Failed to kust directory: " + srcDir);
        }
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
            return decode(buffer, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return decode(buffer, StandardCharsets.ISO_8859_1);
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
            throw new IOException("Failed to invoke parent directory: " + parentDir);
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
        p("    java org.apache.freemarker.core.PrintAST <templateFile>");
        p("    java org.apache.freemarker.core.PrintAST ftl:<templateSource>");
        p("    java org.apache.freemarker.core.PrintAST -r <src-directory> <regexp> <dst-directory>");
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
        Template t = new Template(templateName, ftl, new TestConfigurationBuilder().build());
        return getASTAsString(t, opts);
    }

    public static String getASTAsString(Template t) throws IOException {
        return getASTAsString(t, null);
    }

    public static String getASTAsString(Template t, Options opts) throws IOException {
        validateAST(t);
        
        StringWriter out = new StringWriter();
        printNode(t.getRootASTNode(), "", null, opts != null ? opts : Options.DEFAULT_INSTANCE, out);
        return out.toString();
    }
    
    public static void validateAST(Template t) throws InvalidASTException {
        final ASTElement node = t.getRootASTNode();
        if (node.getParent() != null) {
            throw new InvalidASTException("Root node parent must be null."
                    + "\nRoot node: " + node.dump(false)
                    + "\nParent"
                    + ": " + node.getParent().getClass() + ", " + node.getParent().dump(false));
        }
        validateAST(node);
    }

    private static void validateAST(ASTElement te) {
        int childCount = te.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ASTElement child = te.fastGetChild(i);
            ASTElement parentElement = child.getParent();
            // As ASTImplicitParent.accept does nothing but returns its children, it's optimized out in the final
            // AST tree. While it will be present as a child, the parent element also will have children
            // that contains the children of the ASTImplicitParent directly. 
            if (parentElement instanceof ASTImplicitParent && parentElement.getParent() != null) {
                parentElement = parentElement.getParent();
            }
            if (parentElement != te) {
                throw new InvalidASTException("Wrong parent node."
                        + "\nNode: " + child.dump(false)
                        + "\nExpected parent: " + te.dump(false)
                        + "\nActual parent: " + parentElement.dump(false));
            }
            if (child.getIndex() != i) {
                throw new InvalidASTException("Wrong node index."
                        + "\nNode: " + child.dump(false)
                        + "\nExpected index: " + i
                        + "\nActual index: " + child.getIndex());
            }
        }
        if (te instanceof ASTImplicitParent && te.getChildCount() < 2) {
            throw new InvalidASTException("Mixed content with child count less than 2 should removed by optimizatoin, "
                    + "but found one with " + te.getChildCount() + " child(ren).");
        }
        ASTElement[] children = te.getChildBuffer();
        if (children != null) {
            if (childCount == 0) {
                throw new InvalidASTException(
                        "Children must be null when childCount is 0."
                        + "\nNode: " + te.dump(false));
            }
            for (int i = 0; i < te.getChildCount(); i++) {
                if (children[i] == null) {
                    throw new InvalidASTException(
                            "Child can't be null at index " + i
                            + "\nNode: " + te.dump(false));
                }
            }
            for (int i = te.getChildCount(); i < children.length; i++) {
                if (children[i] != null) {
                    throw new InvalidASTException(
                            "Children can't be non-null at index " + i
                            + "\nNode: " + te.dump(false));
                }
            }
        } else {
            if (childCount != 0) {
                throw new InvalidASTException(
                        "Children mustn't be null when child count isn't 0."
                        + "\nNode: " + te.dump(false));
            }
        }
    }

    private static void printNode(Object node, String ind, ParameterRole paramRole, Options opts, Writer out) throws IOException {
        if (node instanceof ASTNode) {
            ASTNode tObj = (ASTNode) node;

            printNodeLineStart(paramRole, ind, out);
            out.write(tObj.getLabelWithoutParameters());
            printNodeLineEnd(node, out, opts);
            
            if (opts.getShowConstantValue() && node instanceof ASTExpression) {
                TemplateModel tm = ((ASTExpression) node).constantValue;
                if (tm != null) {
                    out.write(INDENTATION);
                    out.write(ind);
                    out.write("= const ");
                    out.write(TemplateLanguageUtils.getTypeDescription(tm));
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
            if (tObj instanceof ASTElement) {
                for (ASTElement child : ((ASTElement) tObj).getChildren()) {
                    printNode(child, INDENTATION + ind, null, opts, out);
                }
            }
        } else {
            printNodeLineStart(paramRole, ind, out);
            out.write(_StringUtils.jQuote(node));
            printNodeLineEnd(node, out, opts);
        }
    }

    protected static void printNodeLineEnd(Object node, Writer out, Options opts) throws IOException {
        boolean commentStared = false;
        if (opts.getShowJavaClass()) {
            out.write("  // ");
            commentStared = true;
            out.write(_ClassUtils.getShortClassNameOfObject(node, true));
        }
        if (opts.getShowLocation() && node instanceof ASTNode) {
            if (!commentStared) {
                out.write("  // ");
                commentStared = true;
            } else {
                out.write("; ");
            }
            ASTNode tObj = (ASTNode) node;
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
