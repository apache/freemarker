package org.apache.freemarker.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Én on 2/26/2017.
 */
public final class _Debug {

    private _Debug() {
        //
    }


    public static void insertDebugBreak(Template t, int line) {
        ASTElement te = findTemplateElement(t.getRootASTNode(), line);
        if (te == null) {
            return;
        }
        ASTElement parent = te.getParent();
        ASTDebugBreak db = new ASTDebugBreak(te);
        // TODO: Ensure there always is a parent by making sure
        // that the root element in the template is always a ASTImplicitParent
        // Also make sure it doesn't conflict with anyone's code.
        parent.setChildAt(parent.getIndex(te), db);
    }

    public static void removeDebugBreak(Template t, int line) {
        ASTElement te = findTemplateElement(t.getRootASTNode(), line);
        if (te == null) {
            return;
        }
        ASTDebugBreak db = null;
        while (te != null) {
            if (te instanceof ASTDebugBreak) {
                db = (ASTDebugBreak) te;
                break;
            }
            te = te.getParent();
        }
        if (db == null) {
            return;
        }
        ASTElement parent = db.getParent();
        parent.setChildAt(parent.getIndex(db), db.getChild(0));
    }

    private static ASTElement findTemplateElement(ASTElement te, int line) {
        if (te.getBeginLine() > line || te.getEndLine() < line) {
            return null;
        }
        // Find the narrowest match
        List childMatches = new ArrayList();
        for (Enumeration children = te.children(); children.hasMoreElements(); ) {
            ASTElement child = (ASTElement) children.nextElement();
            ASTElement childmatch = findTemplateElement(child, line);
            if (childmatch != null) {
                childMatches.add(childmatch);
            }
        }
        //find a match that exactly matches the begin/end line
        ASTElement bestMatch = null;
        for (int i = 0; i < childMatches.size(); i++) {
            ASTElement e = (ASTElement) childMatches.get(i);

            if ( bestMatch == null ) {
                bestMatch = e;
            }

            if ( e.getBeginLine() == line && e.getEndLine() > line ) {
                bestMatch = e;
            }

            if ( e.getBeginLine() == e.getEndLine() && e.getBeginLine() == line) {
                bestMatch = e;
                break;
            }
        }
        if ( bestMatch != null) {
            return bestMatch;
        }
        // If no child provides narrower match, return this
        return te;
    }

    public static void removeDebugBreaks(Template t) {
        removeDebugBreaks(t.getRootASTNode());
    }

    private static void removeDebugBreaks(ASTElement te) {
        int count = te.getChildCount();
        for (int i = 0; i < count; ++i) {
            ASTElement child = te.getChild(i);
            while (child instanceof ASTDebugBreak) {
                ASTElement dbchild = child.getChild(0);
                te.setChildAt(i, dbchild);
                child = dbchild;
            }
            removeDebugBreaks(child);
        }
    }
}
