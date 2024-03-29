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

package freemarker.core;

import java.io.PrintStream;
import java.io.PrintWriter;

import freemarker.template.TemplateException;

/**
 * This exception is thrown when a {@code #stop} directive is encountered. 
 */
public class StopException extends TemplateException {
    
    StopException(Environment env) {
        super(env);
    }

    StopException(Environment env, String s) {
        super(s, env);
    }

    @Override
    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            String msg = this.getMessage();
            pw.print("Encountered stop instruction");
            if (msg != null && !msg.equals("")) {
                pw.println("\nCause given: " + msg);
            } else pw.println();
            super.printStackTrace(pw);
        }
    }

    @Override
    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            String msg = this.getMessage();
            ps.print("Encountered stop instruction");
            if (msg != null && !msg.equals("")) {
                ps.println("\nCause given: " + msg);
            } else ps.println();
            super.printStackTrace(ps);
        }
    }
    
}


