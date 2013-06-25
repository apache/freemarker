package freemarker.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.Version;
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
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
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
        
        File srcDir = new File(args[1]);
        if (!srcDir.isDirectory()) {
            p(StringUtil.jQuote(args[1]) + " must be a directory");
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
        
        File dstDir = new File(args[3]);
        if (!dstDir.isDirectory()) {
            p(StringUtil.jQuote(args[3]) + " must be a directory");
            System.exit(-1);
        }
        
        recurse(srcDir, fnPattern, dstDir);
        
        p("Successfully processed " + successfulCounter + ", failed  " + failedCounter + ".");
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
                        Template t;
                        Reader r = new InputStreamReader(new FileInputStream(file), "ISO-8859-1");
                        try {
                                t = new Template(file.getPath().replace('\\', '/'), r, cfg);
                        } finally {
                            r.close();
                        }
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
        StringWriter out = new StringWriter();
        printNode(t.getRootTreeNode(), "", null, opts != null ? opts : Options.DEFAULT_INSTANCE, out);
        return out.toString();
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
        if (opts.getShowJavaClass()) {
            out.write("  // ");
            out.write(ClassUtil.getShortClassNameOfObject(node, true));
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
        
    }
    
    private static void p(Object obj) {
        System.out.println(obj);
    }

}
