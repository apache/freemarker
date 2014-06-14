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

package freemarker.test;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileReader;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import freemarker.core.FreeMarkerTree;
import freemarker.template.Template;

public class TreeView {

    static public void main(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }
        String filename = args[0];
        Template t = null;
        try {
            t = new Template(filename, new FileReader(filename));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        showTree(t);
    }

    static void showTree(Template t) {
        JTree tree = new FreeMarkerTree(t);
        JFrame jf = new JFrame(t.getName());
        jf.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        JScrollPane scrollPane = new JScrollPane(tree);
        jf.getContentPane().add(scrollPane);
        jf.pack();
        jf.show();
    }

    static void usage() {
        System.err.println("Little toy program to display a compiled template as a tree.");
        System.err.println("Usage: java freemarker.testcase.TreeView <templatefile>");
    }
}
