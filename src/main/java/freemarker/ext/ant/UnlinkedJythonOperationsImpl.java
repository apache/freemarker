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

package freemarker.ext.ant;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.python.util.PythonInterpreter;

/**
 * Used internally, public for technical reasons only. 
 */
public class UnlinkedJythonOperationsImpl implements UnlinkedJythonOperations {
    
    @Override
    public void execute(String script, Map vars) throws BuildException {
        PythonInterpreter pi = createInterpreter(vars);
        pi.exec(script);
    }

    @Override
    public void execute(File file, Map vars) throws BuildException {
        PythonInterpreter pi = createInterpreter(vars);
        try {
            pi.execfile(file.getCanonicalPath());
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
    
    private PythonInterpreter createInterpreter(Map vars) {
        PythonInterpreter pi = new PythonInterpreter();
        Iterator it = vars.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry ent = (Map.Entry) it.next(); 
            pi.set((String) ent.getKey(), ent.getValue());
        }
        return pi;
    }
    
}
