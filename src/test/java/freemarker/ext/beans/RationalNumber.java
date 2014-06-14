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

package freemarker.ext.beans;

public final class RationalNumber extends Number {
    
    final int divident;
    final int divisor;
    
    public RationalNumber(int divident, int divisor) {
        this.divident = divident;
        this.divisor = divisor;
    }

    @Override
    public int intValue() {
        return divident / divisor;
    }

    @Override
    public long longValue() {
        return divident / (long) divisor;
    }

    @Override
    public float floatValue() {
        return (float) (divident / (double) divisor);
    }

    @Override
    public double doubleValue() {
        return divident / (double) divisor;
    }

    public int getDivident() {
        return divident;
    }

    public int getDivisor() {
        return divisor;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + divident;
        result = prime * result + divisor;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RationalNumber other = (RationalNumber) obj;
        if (divident != other.divident)
            return false;
        if (divisor != other.divisor)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return divident + "/" + divisor;
    }
    
}