package freemarker.core;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;

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

    static public void main(String[] args) throws IOException {
        if (args.length == 0) {
            usage();
            return;
        }
        
        Configuration cfg = new Configuration();
        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        
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
        
        System.out.println(getASTAsString(t));
    }
    
    private static void usage() {
        System.err.println("Prints template Abstract Syntax Tree (AST) as plain text.");
        System.err.println("Usage:");
        System.err.println("    java freemarker.core.PrintAST <templateFile>");
        System.err.println("    java freemarker.core.PrintAST ftl:<templateSource>");
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
        Template t = new Template(templateName, new StringReader(ftl), cfg);
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

}
