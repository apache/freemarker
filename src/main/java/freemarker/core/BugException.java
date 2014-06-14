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

/**
 * An unexpected state was reached that is certainly caused by a bug in FreeMarker.
 * 
 * @since 2.3.21
 */
public class BugException extends RuntimeException {

    // Java 5: add the constructors with cause exception.
    
    private static final String COMMON_MESSAGE
        = "A bug was detected in FreeMarker; please report it with stack-trace";

    public BugException() {
        super(COMMON_MESSAGE);
    }

    public BugException(String message) {
        super(COMMON_MESSAGE + ": " + message);
    }

    public BugException(int value) {
        this(String.valueOf(value));
    }

}
