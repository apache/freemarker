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

package freemarker.ext.beans;

import static freemarker.template.Configuration.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.junit.Test;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import freemarker.test.TemplateTest;

public class TestZeroArgumentNonVoidMethodPolicy extends TemplateTest {
    @Override
    protected Configuration createConfiguration() throws Exception {
        Configuration cfg = super.createConfiguration();
        // Don't use default, as then the object wrapper is a shared static mutable object:
        cfg.setIncompatibleImprovements(Configuration.VERSION_2_3_32);
        cfg.setAPIBuiltinEnabled(true);
        return cfg;
    }

    @Test
    public void testDefaultWithHighIncompatibleImprovements() throws TemplateException, IOException {
        for (boolean cacheTopLevelVars : List.of(true, false)){
            setupDataModel(
                    () -> new DefaultObjectWrapper(VERSION_2_3_33),
                    cacheTopLevelVars);
            assertRecIsBothPropertyAndMethod();
            assertNrcIsMethodOnly();
        }
    }

    @Test
    public void testDefaultWithLowIncompatibleImprovements() throws TemplateException, IOException {
        for (boolean cacheTopLevelVars : List.of(true, false)) {
            setupDataModel(
                    () -> new DefaultObjectWrapper(VERSION_2_3_32),
                    cacheTopLevelVars);
            assertRecIsMethodOnly();
            assertNrcIsMethodOnly();
        }
    }

    @Test
    public void testDefaultWithLowIncompatibleImprovements2() throws TemplateException, IOException {
        for (boolean cacheTopLevelVars : List.of(true, false)) {
            setupDataModel(
                    () -> {
                        DefaultObjectWrapper beansWrapper = new DefaultObjectWrapper(VERSION_2_3_32);
                        beansWrapper.setRecordZeroArgumentNonVoidMethodPolicy(
                                ZeroArgumentNonVoidMethodPolicy.BOTH_PROPERTY_AND_METHOD);
                        return beansWrapper;
                    },
                    cacheTopLevelVars);
            assertRecIsBothPropertyAndMethod();
            assertNrcIsMethodOnly();
        }
    }

    @Test
    public void testDefaultWithRecordsPropertyOnly() throws TemplateException, IOException {
        for (boolean cacheTopLevelVars : List.of(true, false)) {
            setupDataModel(
                    () -> {
                        DefaultObjectWrapper beansWrapper = new DefaultObjectWrapper(VERSION_2_3_32);
                        beansWrapper.setRecordZeroArgumentNonVoidMethodPolicy(ZeroArgumentNonVoidMethodPolicy.PROPERTY_ONLY);
                        return beansWrapper;
                    },
                    cacheTopLevelVars);
            assertRecIsPropertyOnly();
            assertNrcIsMethodOnly();
        }
    }

    @Test
    public void testDefaultWithRecordsPropertyOnly2() throws TemplateException, IOException {
        for (boolean cacheTopLevelVars : List.of(true, false)) {
            setupDataModel(
                    () -> {
                        DefaultObjectWrapper beansWrapper = new DefaultObjectWrapper(VERSION_2_3_33);
                        beansWrapper.setRecordZeroArgumentNonVoidMethodPolicy(ZeroArgumentNonVoidMethodPolicy.PROPERTY_ONLY);
                        return beansWrapper;
                    },
                    cacheTopLevelVars);
            assertRecIsPropertyOnly();
            assertNrcIsMethodOnly();
        }
    }

    @Test
    public void testDefaultWithNonRecordsPropertyOnly() throws TemplateException, IOException {
        for (boolean cacheTopLevelVars : List.of(true, false)) {
            setupDataModel(
                    () -> {
                        DefaultObjectWrapper beansWrapper = new DefaultObjectWrapper(VERSION_2_3_32);
                        beansWrapper.setNonRecordZeroArgumentNonVoidMethodPolicy(ZeroArgumentNonVoidMethodPolicy.PROPERTY_ONLY);
                        return beansWrapper;
                    },
                    cacheTopLevelVars);
            assertRecIsMethodOnly();
            assertNrcIsPropertyOnly();
        }
    }

    @Test
    public void testDefaultWithBothPropertyAndMethod() throws TemplateException, IOException {
        for (boolean cacheTopLevelVars : List.of(true, false)) {
            setupDataModel(
                    () -> {
                        DefaultObjectWrapper beansWrapper = new DefaultObjectWrapper(VERSION_2_3_33);
                        beansWrapper.setNonRecordZeroArgumentNonVoidMethodPolicy(
                                ZeroArgumentNonVoidMethodPolicy.BOTH_PROPERTY_AND_METHOD);
                        return beansWrapper;
                    },
                    cacheTopLevelVars);
            assertRecIsBothPropertyAndMethod();
            assertNrcIsBothPropertyAndMethod();
        }
    }

    @Test
    public void testSettings() throws TemplateException, IOException {
            getConfiguration().setSetting(
                    "objectWrapper",
                    "DefaultObjectWrapper(2.3.33, nonRecordZeroArgumentNonVoidMethodPolicy=freemarker.ext.beans.ZeroArgumentNonVoidMethodPolicy.BOTH_PROPERTY_AND_METHOD)");
            setupDataModel(() -> getConfiguration().getObjectWrapper(), false);
            assertRecIsBothPropertyAndMethod();
            assertNrcIsBothPropertyAndMethod();
    }

    private void setupDataModel(Supplier<? extends ObjectWrapper> objectWrapperSupplier, boolean cacheTopLevelVars) {
        ObjectWrapper objectWrapper = objectWrapperSupplier.get();
        getConfiguration().setObjectWrapper(objectWrapper);

        setDataModel(cacheTopLevelVars ? new SimpleHash(objectWrapper) : new HashMap<>());

        addToDataModel("rec", new TestRecord(1, "S"));
        addToDataModel("nrc", new TestNonRecord());
    }

    private void assertRecIsBothPropertyAndMethod() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertOutput(modifyTemplate("${rec.x}", tempMods), "1");
            assertOutput(modifyTemplate("${rec.x()}", tempMods), "1");
            assertOutput(modifyTemplate("${rec.s}", tempMods), "S");
            assertOutput(modifyTemplate("${rec.s()}", tempMods), "S");
            assertOutput(modifyTemplate("${rec.y}", tempMods), "2");
            assertOutput(modifyTemplate("${rec.y()}", tempMods), "2");
            assertOutput(modifyTemplate("${rec.tenX}", tempMods), "10");
            assertOutput(modifyTemplate("${rec.tenX()}", tempMods), "10");
        }
        assertRecPolicyIndependentMembers();
    }

    private void assertRecIsMethodOnly() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertErrorContains(modifyTemplate("${rec.x}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${rec.x()}", tempMods), "1");
            assertErrorContains(modifyTemplate("${rec.s}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${rec.s()}", tempMods), "S");
            assertErrorContains(modifyTemplate("${rec.y}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${rec.y()}", tempMods), "2");
            assertErrorContains(modifyTemplate("${rec.tenX}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${rec.tenX()}", tempMods), "10");
        }
        assertRecPolicyIndependentMembers();
    }

    private void assertRecIsPropertyOnly() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertOutput(modifyTemplate("${rec.x}", tempMods), "1");
            assertErrorContains(modifyTemplate("${rec.x()}", tempMods), "SimpleNumber", "must not be called as a method");
            assertOutput(modifyTemplate("${rec.s}", tempMods), "S");
            assertErrorContains(modifyTemplate("${rec.s()}", tempMods), "SimpleScalar");
            assertOutput(modifyTemplate("${rec.y}", tempMods), "2");
            assertErrorContains(modifyTemplate("${rec.y()}", tempMods), "SimpleNumber");
            assertOutput(modifyTemplate("${rec.tenX}", tempMods), "10");
            assertErrorContains(modifyTemplate("${rec.tenX()}", tempMods), "SimpleNumber");
        }
        assertRecPolicyIndependentMembers();
    }

    private void assertRecPolicyIndependentMembers() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertOutput(modifyTemplate("${rec.z}", tempMods), "3");
            assertErrorContains(modifyTemplate("${rec.z()}", tempMods), "SimpleNumber");
            assertOutput(modifyTemplate("${rec.getZ()}", tempMods), "3");
            assertOutput(modifyTemplate("${rec.xTimes(5)}", tempMods), "5");
            assertErrorContains(modifyTemplate("${rec.xTimes}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${rec.voidMethod()}", tempMods), "");
            assertErrorContains(modifyTemplate("${rec.voidMethod}", tempMods), "SimpleMethodModel");
        }
    }

    private void assertNrcIsMethodOnly() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertErrorContains(modifyTemplate("${nrc.x}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${nrc.x()}", tempMods), "1");
            assertErrorContains(modifyTemplate("${nrc.y}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${nrc.y()}", tempMods), "2");
            assertErrorContains(modifyTemplate("${nrc.tenX}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${nrc.tenX()}", tempMods), "10");
        }
        assertNrcPolicyIndependentMembers();
    }

    private void assertNrcIsBothPropertyAndMethod() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertOutput(modifyTemplate("${nrc.x}", tempMods), "1");
            assertOutput(modifyTemplate("${nrc.x()}", tempMods), "1");
            assertOutput(modifyTemplate("${nrc.y}", tempMods), "2");
            assertOutput(modifyTemplate("${nrc.y()}", tempMods), "2");
            assertOutput(modifyTemplate("${nrc.tenX}", tempMods), "10");
            assertOutput(modifyTemplate("${nrc.tenX()}", tempMods), "10");
        }
        assertNrcPolicyIndependentMembers();
    }

    private void assertNrcIsPropertyOnly() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertOutput(modifyTemplate("${nrc.x}", tempMods), "1");
            assertErrorContains(modifyTemplate("${nrc.x()}", tempMods), "SimpleNumber", "must not be called as a method");
            assertOutput(modifyTemplate("${nrc.y}", tempMods), "2");
            assertErrorContains(modifyTemplate("${nrc.y()}", tempMods), "SimpleNumber");
            assertOutput(modifyTemplate("${nrc.tenX}", tempMods), "10");
            assertErrorContains(modifyTemplate("${nrc.tenX()}", tempMods), "SimpleNumber");
        }
        assertNrcPolicyIndependentMembers();
    }

    private void assertNrcPolicyIndependentMembers() throws IOException, TemplateException {
        for (TemplateModifications tempMods : TemplateModifications.values()) {
            assertOutput(modifyTemplate("${nrc.z}", tempMods), "3");
            assertErrorContains(modifyTemplate("${nrc.z()}", tempMods), "SimpleNumber");
            assertOutput(modifyTemplate("${nrc.getZ()}", tempMods), "3");
            assertOutput(modifyTemplate("${nrc.xTimes(5)}", tempMods), "5");
            assertErrorContains(modifyTemplate("${nrc.xTimes}", tempMods), "SimpleMethodModel");
            assertOutput(modifyTemplate("${nrc.voidMethod()}", tempMods), "");
            assertErrorContains(modifyTemplate("${nrc.voidMethod}", tempMods), "SimpleMethodModel");
        }
    }

    public interface TestInterface {
        int y();

        /**
         * Defines a real JavaBeans property, "z", so the {@link ZeroArgumentNonVoidMethodPolicy} shouldn't affect this
         */
        int getZ();
    }

    /**
     * Defines record component readers for "x" and "s", and some other non-record-component methods that are still
     * potentially exposed as if there were properties.
     */
    public record TestRecord(int x, String s) implements TestInterface {
        @Override
        public int y() {
            return 2;
        }

        @Override
        public int getZ() {
            return 3;
        }

        public int tenX() {
            return x * 10;
        }

        /**
         * Has an argument, so this never should be exposed as property.
         */
        public int xTimes(int m) {
            return x * m;
        }

        /**
         * Has a void return type, so this never should be exposed as property.
         */
        public void voidMethod() {
            // do nothing
        }
    }

    public static class TestNonRecord implements TestInterface {
        public int x() {
            return 1;
        }

        @Override
        public int y() {
            return 2;
        }

        @Override
        public int getZ() {
            return 3;
        }

        public int tenX() {
            return x() * 10;
        }

        public int xTimes(int m) {
            return x() * m;
        }

        /**
         * Has a void return type, so this never should be exposed as property.
         */
        public void voidMethod() {
            // do nothing
        }
    }

    private static final Pattern DOT_TO_SQUARE_BRACKETS_REPLACEMENT_PATTERN = Pattern.compile("\\.(\\w+)");
    
    private static String modifyTemplate(String s, TemplateModifications tempMods) {
        if (tempMods.useApi) {
            s = s.replace(".", "?api.");
        }
        if (tempMods.doToSquareBrackets) {
            s = DOT_TO_SQUARE_BRACKETS_REPLACEMENT_PATTERN.matcher(s).replaceFirst(key -> "['" + key.group(1) + "']");
        }
        return s;
    }

    enum TemplateModifications {
        DOT(true, false), SQUARE_BRACKETS(false, false),
        API_DOT(true, true), API_SQUARE_BRACKETS(false, true);

        private final boolean doToSquareBrackets;
        private final boolean useApi;

        TemplateModifications(boolean doToSquareBrackets, boolean useApi) {
            this.doToSquareBrackets = doToSquareBrackets;
            this.useApi = useApi;
        }
    }


}
