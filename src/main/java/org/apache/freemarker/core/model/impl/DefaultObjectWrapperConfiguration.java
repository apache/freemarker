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

import org.apache.freemarker.core.Version;
import org.apache.freemarker.core._CoreAPI;
import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.core.model.TemplateDateModel;

/**
 * Holds {@link DefaultObjectWrapper} configuration settings and defines their defaults.
 * You will not use this abstract class directly, but concrete subclasses like {@link DefaultObjectWrapperBuilder}.
 * Unless, you are developing a builder for a custom {@link DefaultObjectWrapper} subclass. In that case, note that
 * overriding the {@link #equals} and {@link #hashCode} is important, as these objects are used as {@link ObjectWrapper}
 * singleton lookup keys.
 */
public abstract class DefaultObjectWrapperConfiguration implements Cloneable {

    private final Version incompatibleImprovements;

    ClassIntrospectorBuilder classIntrospectorBuilder;

    // Properties and their *defaults*:
    private boolean simpleMapWrapper = false;
    private int defaultDateType = TemplateDateModel.UNKNOWN;
    private ObjectWrapper outerIdentity = null;
    private boolean strict = false;
    private boolean useModelCache = false;
    // Attention!
    // - As this object is a cache key, non-normalized field values should be avoided.
    // - Fields with default values must be set until the end of the constructor to ensure that when the lookup happens,
    //   there will be no unset fields.
    // - If you add a new field, review all methods in this class

    /**
     * @param incompatibleImprovements
     *            See the corresponding parameter of {@link DefaultObjectWrapper#DefaultObjectWrapper(Version)}. Not {@code null}. Note
     *            that the version will be normalized to the lowest version where the same incompatible
     *            {@link DefaultObjectWrapper} improvements were already present, so for the returned instance
     *            {@link #getIncompatibleImprovements()} might returns a lower version than what you have specified
     *            here.
     * @param isIncompImprsAlreadyNormalized
     *            Tells if the {@code incompatibleImprovements} parameter contains an <em>already normalized</em> value.
     *            This parameter meant to be {@code true} when the class that extends {@link DefaultObjectWrapper} needs to add
     *            additional breaking versions over those of {@link DefaultObjectWrapper}. Thus, if this parameter is
     *            {@code true}, the versions where {@link DefaultObjectWrapper} had breaking changes must be already factored
     *            into the {@code incompatibleImprovements} parameter value, as no more normalization will happen. (You
     *            can use {@link DefaultObjectWrapper#normalizeIncompatibleImprovementsVersion(Version)} to discover those.)
     *
     * @since 2.3.22
     */
    protected DefaultObjectWrapperConfiguration(Version incompatibleImprovements, boolean isIncompImprsAlreadyNormalized) {
        _CoreAPI.checkVersionNotNullAndSupported(incompatibleImprovements);

        incompatibleImprovements = isIncompImprsAlreadyNormalized
                ? incompatibleImprovements
                : DefaultObjectWrapper.normalizeIncompatibleImprovementsVersion(incompatibleImprovements);
        this.incompatibleImprovements = incompatibleImprovements;

        classIntrospectorBuilder = new ClassIntrospectorBuilder(incompatibleImprovements);
    }

    /**
     * Same as {@link #DefaultObjectWrapperConfiguration(Version, boolean) DefaultObjectWrapperConfiguration(Version, false)}.
     */
    protected DefaultObjectWrapperConfiguration(Version incompatibleImprovements) {
        this(incompatibleImprovements, false);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + incompatibleImprovements.hashCode();
        result = prime * result + (simpleMapWrapper ? 1231 : 1237);
        result = prime * result + defaultDateType;
        result = prime * result + (outerIdentity != null ? outerIdentity.hashCode() : 0);
        result = prime * result + (strict ? 1231 : 1237);
        result = prime * result + (useModelCache ? 1231 : 1237);
        result = prime * result + classIntrospectorBuilder.hashCode();
        return result;
    }

    /**
     * Two {@link DefaultObjectWrapperConfiguration}-s are equal exactly if their classes are identical ({@code ==}), and their
     * field values are equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DefaultObjectWrapperConfiguration other = (DefaultObjectWrapperConfiguration) obj;

        if (!incompatibleImprovements.equals(other.incompatibleImprovements)) return false;
        if (simpleMapWrapper != other.simpleMapWrapper) return false;
        if (defaultDateType != other.defaultDateType) return false;
        if (outerIdentity != other.outerIdentity) return false;
        if (strict != other.strict) return false;
        if (useModelCache != other.useModelCache) return false;
        return classIntrospectorBuilder.equals(other.classIntrospectorBuilder);
    }

    protected Object clone(boolean deepCloneKey) {
        try {
            DefaultObjectWrapperConfiguration clone = (DefaultObjectWrapperConfiguration) super.clone();
            if (deepCloneKey) {
                clone.classIntrospectorBuilder
                        = (ClassIntrospectorBuilder) classIntrospectorBuilder.clone();
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone DefaultObjectWrapperConfiguration", e);
        }
    }

    public int getDefaultDateType() {
        return defaultDateType;
    }

    /** See {@link DefaultObjectWrapper#setDefaultDateType(int)}. */
    public void setDefaultDateType(int defaultDateType) {
        this.defaultDateType = defaultDateType;
    }

    public ObjectWrapper getOuterIdentity() {
        return outerIdentity;
    }

    /**
     * See {@link DefaultObjectWrapper#setOuterIdentity(ObjectWrapper)}, except here the default is {@code null} that means
     * the {@link ObjectWrapper} that you will set up with this {@link DefaultObjectWrapperBuilder} object.
     */
    public void setOuterIdentity(ObjectWrapper outerIdentity) {
        this.outerIdentity = outerIdentity;
    }

    public boolean isStrict() {
        return strict;
    }

    /** See {@link DefaultObjectWrapper#setStrict(boolean)}. */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean getUseModelCache() {
        return useModelCache;
    }

    /** See {@link DefaultObjectWrapper#setUseModelCache(boolean)} (it means the same). */
    public void setUseModelCache(boolean useModelCache) {
        this.useModelCache = useModelCache;
    }

    public Version getIncompatibleImprovements() {
        return incompatibleImprovements;
    }

    public int getExposureLevel() {
        return classIntrospectorBuilder.getExposureLevel();
    }

    /** See {@link DefaultObjectWrapper#setExposureLevel(int)}. */
    public void setExposureLevel(int exposureLevel) {
        classIntrospectorBuilder.setExposureLevel(exposureLevel);
    }

    public boolean getExposeFields() {
        return classIntrospectorBuilder.getExposeFields();
    }

    /** See {@link DefaultObjectWrapper#setExposeFields(boolean)}. */
    public void setExposeFields(boolean exposeFields) {
        classIntrospectorBuilder.setExposeFields(exposeFields);
    }

    public MethodAppearanceFineTuner getMethodAppearanceFineTuner() {
        return classIntrospectorBuilder.getMethodAppearanceFineTuner();
    }

    /**
     * See {@link DefaultObjectWrapper#setMethodAppearanceFineTuner(MethodAppearanceFineTuner)}; additionally,
     * note that currently setting this to non-{@code null} will disable class introspection cache sharing, unless
     * the value implements {@link SingletonCustomizer}.
     */
    public void setMethodAppearanceFineTuner(MethodAppearanceFineTuner methodAppearanceFineTuner) {
        classIntrospectorBuilder.setMethodAppearanceFineTuner(methodAppearanceFineTuner);
    }

    MethodSorter getMethodSorter() {
        return classIntrospectorBuilder.getMethodSorter();
    }

    void setMethodSorter(MethodSorter methodSorter) {
        classIntrospectorBuilder.setMethodSorter(methodSorter);
    }

}
