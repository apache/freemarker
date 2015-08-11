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

import java.io.IOException;
import java.io.Writer;

import freemarker.template.TemplateModelException;


public final class HTMLOutputFormat extends OutputFormat<HTMLTemplateOutputModel> {

    public static final HTMLOutputFormat INSTANCE = new HTMLOutputFormat();
    
    private static final char[] LT = new char[] { '&', 'l', 't', ';' };
    private static final char[] GT = new char[] { '&', 'g', 't', ';' };
    private static final char[] AMP = new char[] { '&', 'a', 'm', 'p', ';' };
    private static final char[] QUOT = new char[] { '&', 'q', 'u', 'o', 't', ';' };
    private static final char[] APOS = new char[] { '&', '#', '3', '9', ';' };
    
    private HTMLOutputFormat() {
        // Only to decrease visibility
    }
    
    @Override
    void output(HTMLTemplateOutputModel tom, Writer out) throws IOException, TemplateModelException {
        String mc = tom.getMarkupContent();
        if (mc != null) {
            out.write(mc);
        } else {
            output(tom.getPlainTextContent(), out);
        }
    }

    @Override
    void output(String textToEsc, Writer out) throws IOException, TemplateModelException {
        int writtenEnd = 0;  // exclusive end
        int ln = textToEsc.length();
        for (int i = 0; i < ln; i++) {
            char c = textToEsc.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                int flushLn = i - writtenEnd;
                if (flushLn != 0) {
                    out.write(textToEsc, writtenEnd, flushLn);
                }
                writtenEnd = i + 1;
                
                switch (c) {
                case '<': out.write(LT); break;
                case '>': out.write(GT); break;
                case '&': out.write(AMP); break;
                case '"': out.write(QUOT); break;
                default: out.write(APOS); break;
                }
            }
        }
        if (writtenEnd < ln) {
            out.write(textToEsc, writtenEnd, ln - writtenEnd);
        }
    }

    @Override
    HTMLTemplateOutputModel escapePlainText(String textToEsc) throws TemplateModelException {
        return new HTMLTemplateOutputModel(textToEsc, null);
    }

    @Override
    String getSourcePlainText(HTMLTemplateOutputModel tom) {
        return tom.getPlainTextContent();
    }

    @Override
    HTMLTemplateOutputModel fromMarkup(String markupText) throws TemplateModelException {
        return new HTMLTemplateOutputModel(null, markupText);
    }

    @Override
    String getMarkup(HTMLTemplateOutputModel tom) {
        String mc = tom.getMarkupContent();
        if (mc != null) {
            return mc;
        }
        
        String pc = tom.getPlainTextContent();
        int ln = pc.length();
        
        // First we find out if we need to escape, and if so, what the length of the output will be:
        int firstEscIdx = -1;
        int lastEscIdx = 0;
        int plusOutLn = 0;
        for (int i = 0; i < ln; i++) {
            char c = pc.charAt(i);
            if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                if (firstEscIdx == -1) {
                    firstEscIdx = i;
                }
                lastEscIdx = i;
                
                switch (c) {
                case '<': plusOutLn += LT.length - 1; break;
                case '>': plusOutLn += GT.length - 1; break;
                case '&': plusOutLn += AMP.length - 1; break;
                case '"': plusOutLn += QUOT.length - 1; break;
                default: plusOutLn += APOS.length - 1; break;
                }
            }
        }
        
        if (firstEscIdx == -1) {
            return pc; // Nothing to escape
        } else {
            char[] esced = new char[ln + plusOutLn];
            if (firstEscIdx != 0) {
                pc.getChars(0, firstEscIdx, esced, 0);
            }
            int dst = firstEscIdx;
            for (int i = firstEscIdx; i <= lastEscIdx; i++) {
                char c = pc.charAt(i);
                if (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'') {
                    switch (c) {
                    case '<': dst = shortArrayCopy(LT, esced, dst); break;
                    case '>': dst = shortArrayCopy(GT, esced, dst); break;
                    case '&': dst = shortArrayCopy(AMP, esced, dst); break;
                    case '"': dst = shortArrayCopy(QUOT, esced, dst); break;
                    default: dst = shortArrayCopy(APOS, esced, dst); break;
                    }
                } else {
                    esced[dst++] = c;
                }
            }
            if (lastEscIdx != ln - 1) {
                pc.getChars(lastEscIdx + 1, ln, esced, dst);
            }
            
            return String.valueOf(esced);
        }
    }
    
    @Override
    String getMimeType() {
        return "text/html";
    }

    @Override
    boolean isLegacyBuiltInBypassed(String builtInName) {
        return builtInName.equals("html") || builtInName.equals("xml") || builtInName.equals("xhtml");
    }

    @Override
    HTMLTemplateOutputModel concat(HTMLTemplateOutputModel tom1, HTMLTemplateOutputModel tom2) {
        String pc1 = tom1.getPlainTextContent();
        String mc1 = tom1.getMarkupContent();
        String pc2 = tom2.getPlainTextContent();
        String mc2 = tom2.getMarkupContent();
        
        String pc3 = pc1 != null && pc2 != null ? pc1 + pc2 : null;
        String mc3 = mc1 != null && mc2 != null ? mc1 + mc2 : null;
        if (pc3 != null || mc3 != null) {
            return new HTMLTemplateOutputModel(pc3, mc3);
        }
        
        if (pc1 != null) {
            return new HTMLTemplateOutputModel(null, getMarkup(tom1) + mc2);
        } else {
            return new HTMLTemplateOutputModel(null, mc1 + getMarkup(tom2));
        }
    }

    /**
     * For efficiently copying very short char arrays.
     */
    private static int shortArrayCopy(char[] src, char[] dst, int dstOffset) {
        int ln = src.length;
        for (int i = 0; i < ln; i++) {
            dst[dstOffset++] = src[i];
        }
        return dstOffset;
    }

}
