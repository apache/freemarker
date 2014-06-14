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

package freemarker.template;

import java.io.Serializable;


/**
 * A simple implementation of the <tt>TemplateNumberModel</tt>
 * interface. Note that this class is immutable.
 *
 * <p>This class is thread-safe.
 */
public final class SimpleNumber implements TemplateNumberModel, Serializable {

    /**
     * @serial the value of this <tt>SimpleNumber</tt> 
     */
    private final Number value;

    public SimpleNumber(Number value) {
        this.value = value;
    }

    public SimpleNumber(byte val) {
        this.value = new Byte(val);
    }

    public SimpleNumber(short val) {
        this.value = new Short(val);
    }

    public SimpleNumber(int val) {
        this.value = new Integer(val);
    }

    public SimpleNumber(long val) {
        this.value = new Long(val);
    }

    public SimpleNumber(float val) {
        this.value = new Float(val);
    }
    
    public SimpleNumber(double val) {
        this.value = new Double(val);
    }

    public Number getAsNumber() {
        return value;
    }

    public String toString() {
        return value.toString();
    }
}
