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

package freemarker.ext.ant;

import java.io.File;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;

import freemarker.template.utility.ClassUtil;

/**
 * Used internally, public for technical reasons only.
 * 
 * <p>This is an Ant sub-task of the {@link FreemarkerXmlTask} task.
 * 
 * <p><b>Warning! It must not be statically linked with Jython classes</b>,
 * so users can use the XML ant task even if Jython is not installed. 
 */
public class JythonAntTask extends Task {
    
    private File scriptFile;
    private String script = "";
    private UnlinkedJythonOperations jythonOps;
    
    public void setFile(File scriptFile) throws BuildException {
        ensureJythonOpsExists();
        this.scriptFile = scriptFile;
    }
    
    public void addText(String text) {
        script += text;
    }
    
    public void execute(Map vars) throws BuildException {
        if (scriptFile != null) {
            ensureJythonOpsExists();
            jythonOps.execute(scriptFile, vars);
        } 
        if (script.trim().length() >0) {
            ensureJythonOpsExists();
            String finalScript = ProjectHelper.replaceProperties(
                    project, script, project.getProperties());
            jythonOps.execute(finalScript, vars);
        }
    }

    private void ensureJythonOpsExists() {
        if (jythonOps == null) {
            Class clazz;
            try {
                clazz = ClassUtil.forName(
                        "freemarker.ext.ant.UnlinkedJythonOperationsImpl");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                        "A ClassNotFoundException has been thrown when trying "
                        + "to get the "
                        + "freemarker.ext.ant.UnlinkedJythonOperationsImpl class. "
                        + "The error message was: " + e.getMessage());
            }
            try {
                jythonOps
                        = (UnlinkedJythonOperations) clazz.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                        "An exception has been thrown when trying "
                        + "to create a freemarker.ext.ant.JythonAntTask "
                        + "object. The exception was: " + e);
            }
        }
    }
    
}
