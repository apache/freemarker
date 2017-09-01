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

package org.apache.freemarker.core;

import java.math.BigInteger;

import org.apache.freemarker.core.model.TemplateIterableModel;
import org.apache.freemarker.core.model.TemplateModel;
import org.apache.freemarker.core.model.TemplateModelIterator;
import org.apache.freemarker.core.model.impl.SimpleNumber;

/**
 * This is the model used for right-unbounded ranges
 */
final class RightUnboundedRangeModel extends RangeModel implements TemplateIterableModel {

    RightUnboundedRangeModel(int begin) {
        super(begin);
    }

    @Override
    final int getStep() {
        return 1;
    }

    @Override
    final boolean isRightUnbounded() {
        return true;
    }

    @Override
    final boolean isRightAdaptive() {
        return true;
    }

    @Override
    public int getCollectionSize() throws TemplateException {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isEmptyCollection() throws TemplateException {
        return false;
    }

    @Override
    public TemplateModelIterator iterator() throws TemplateException {
        return new TemplateModelIterator() {
            boolean needInc;
            int nextType = 1;
            int nextInt = getBeginning();
            long nextLong;
            BigInteger nextBigInteger;

            @Override
            public TemplateModel next() throws TemplateException {
                if (needInc) {
                    switch (nextType) {
                    case 1:
                        if (nextInt < Integer.MAX_VALUE) {
                            nextInt++;
                        } else {
                            nextType = 2;
                            nextLong = nextInt + 1L;
                        }
                        break;
                        
                    case 2:
                        if (nextLong < Long.MAX_VALUE) {
                            nextLong++;
                        } else {
                            nextType = 3;
                            nextBigInteger = BigInteger.valueOf(nextLong);
                            nextBigInteger = nextBigInteger.add(BigInteger.ONE);
                        }
                        break;
                        
                    default: // 3
                        nextBigInteger = nextBigInteger.add(BigInteger.ONE);
                    }
                }
                needInc = true;
                return nextType == 1 ? new SimpleNumber(nextInt)
                        : (nextType == 2 ? new SimpleNumber(nextLong)
                        : new SimpleNumber(nextBigInteger)); 
            }

            @Override
            public boolean hasNext() throws TemplateException {
                return true;
            }
            
        };
        
    }
    
}
