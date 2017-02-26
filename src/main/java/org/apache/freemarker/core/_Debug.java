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
        _ASTElement te = findTemplateElement(t.getRootTreeNode(), line);
        if (te == null) {
            return;
        }
        _ASTElement parent = te.getParent();
        _ASTDebugBreak db = new _ASTDebugBreak(te);
        // TODO: Ensure there always is a parent by making sure
        // that the root element in the template is always a ASTImplicitParent
        // Also make sure it doesn't conflict with anyone's code.
        parent.setChildAt(parent.getIndex(te), db);
    }

    public static void removeDebugBreak(Template t, int line) {
        _ASTElement te = findTemplateElement(t.getRootTreeNode(), line);
        if (te == null) {
            return;
        }
        _ASTDebugBreak db = null;
        while (te != null) {
            if (te instanceof _ASTDebugBreak) {
                db = (_ASTDebugBreak) te;
                break;
            }
            te = te.getParent();
        }
        if (db == null) {
            return;
        }
        _ASTElement parent = db.getParent();
        parent.setChildAt(parent.getIndex(db), db.getChild(0));
    }

    private static _ASTElement findTemplateElement(_ASTElement te, int line) {
        if (te.getBeginLine() > line || te.getEndLine() < line) {
            return null;
        }
        // Find the narrowest match
        List childMatches = new ArrayList();
        for (Enumeration children = te.children(); children.hasMoreElements(); ) {
            _ASTElement child = (_ASTElement) children.nextElement();
            _ASTElement childmatch = findTemplateElement(child, line);
            if (childmatch != null) {
                childMatches.add(childmatch);
            }
        }
        //find a match that exactly matches the begin/end line
        _ASTElement bestMatch = null;
        for (int i = 0; i < childMatches.size(); i++) {
            _ASTElement e = (_ASTElement) childMatches.get(i);

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
        removeDebugBreaks(t.getRootTreeNode());
    }

    private static void removeDebugBreaks(_ASTElement te) {
        int count = te.getChildCount();
        for (int i = 0; i < count; ++i) {
            _ASTElement child = te.getChild(i);
            while (child instanceof _ASTDebugBreak) {
                _ASTElement dbchild = child.getChild(0);
                te.setChildAt(i, dbchild);
                child = dbchild;
            }
            removeDebugBreaks(child);
        }
    }
}
