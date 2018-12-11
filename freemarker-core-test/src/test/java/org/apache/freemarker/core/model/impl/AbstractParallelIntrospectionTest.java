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

package org.apache.freemarker.core.model.impl;

import org.apache.freemarker.core.Configuration;
import org.apache.freemarker.core.NonTemplateCallPlace;
import org.apache.freemarker.core.TemplateException;
import org.apache.freemarker.core.model.TemplateHashModel;
import org.apache.freemarker.core.model.TemplateNumberModel;
import org.apache.freemarker.core.util.CallableUtils;

import junit.framework.TestCase;

public abstract class AbstractParallelIntrospectionTest extends TestCase {
    
    private static final int NUM_THREADS = 8;
    private static final int NUM_ENTITYES = 8;
    private static final int NUM_MEMBERS = 8;
    private static final int ITERATIONS = 20000;
    private static final double CACHE_CLEARING_CHANCE = 0.01;
    
    private DefaultObjectWrapper ow = new DefaultObjectWrapper.Builder(Configuration.VERSION_3_0_0)
            .usePrivateCaches(true).build();
    
    public AbstractParallelIntrospectionTest(String name) {
        super(name);
    }
    
    public void testReliability() {
        testReliability(ITERATIONS);
    }
    
    public void testReliability(int iterations) {
        TestThread[] ts = new TestThread[NUM_THREADS]; 
        for (int i = 0; i < NUM_THREADS; i++) {
            ts[i] = new TestThread(iterations);
            ts[i].start();
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            try {
                ts[i].join();
                if (ts[i].error != null) {
                    throw new AssertionError(ts[i].error);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected abstract TemplateHashModel getWrappedEntity(int objIdx) throws TemplateException;
    
    protected final DefaultObjectWrapper getObjectWrapper() {
        return ow;
    }
    
    private class TestThread extends Thread {
        
        private final int iterations;
        
        private Throwable error;
        
        private TestThread(int iterations) {
            this.iterations = iterations;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < iterations; i++) {
                    if (Math.random() < CACHE_CLEARING_CHANCE) {
                        ow.clearClassIntrospecitonCache();
                    }
                    int objIdx = (int) (Math.random() * NUM_ENTITYES);
                    TemplateHashModel h = getWrappedEntity(objIdx);
                    int mIdx = (int) (Math.random() * NUM_MEMBERS);
                    testProperty(h, objIdx, mIdx);
                    testMethod(h, objIdx, mIdx);
                }
            } catch (Throwable e) {
                error = e;
            }
        }

        private void testProperty(TemplateHashModel h, int objIdx, int mIdx)
                throws TemplateException, AssertionError {
            TemplateNumberModel pv = (TemplateNumberModel) h.get("p" + mIdx);
            final int expected = objIdx * 1000 + mIdx;
            final int got = pv.getAsNumber().intValue();
            if (got != expected) {
                throw new AssertionError("Property assertation failed; " +
                        "expected " + expected + ", but got " + got);
            }
        }

        private void testMethod(TemplateHashModel h, int objIdx, int mIdx)
                throws TemplateException, AssertionError {
            JavaMethodModel pv = (JavaMethodModel) h.get("m" + mIdx);
            final int expected = objIdx * 1000 + mIdx;
            final int got = ((TemplateNumberModel) pv.execute(
                    CallableUtils.EMPTY_TEMPLATE_MODEL_ARRAY, NonTemplateCallPlace.INSTANCE))
                    .getAsNumber().intValue();
            if (got != expected) {
                throw new AssertionError("Method assertion failed; " +
                        "expected " + expected + ", but got " + got);
            }
        }
        
    }

}
