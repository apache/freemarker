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

/**
 * This is an interface to do Jython operations, which has no dependecy on
 * Jython classes (hence "unlinked").
 * 
 * @see JythonAntTask
 */
interface UnlinkedJythonOperations {

    void execute(String script, Map variables) throws BuildException;
    
    void execute(File file, Map variables) throws BuildException;
    
}
