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
package org.apache.freemarker.servlet.jsp;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;

import org.apache.freemarker.core.model.ObjectWrapper;
import org.apache.freemarker.servlet.FreemarkerServlet;
import org.apache.freemarker.servlet.jsp.TaglibFactory.ClasspathMetaInfTldSource;
import org.apache.freemarker.servlet.jsp.TaglibFactory.ClearMetaInfTldSource;
import org.apache.freemarker.servlet.jsp.TaglibFactory.MetaInfTldSource;
import org.apache.freemarker.servlet.jsp.TaglibFactory.WebInfPerLibJarMetaInfTldSource;

public class TaglibFactoryBuilder {

    private final ServletContext servletContext;
    private final ObjectWrapper objectWrapper;
    private List<MetaInfTldSource> metaInfTldSources = new ArrayList<>();
    private List<String> classPathTlds = new ArrayList<>();

    public TaglibFactoryBuilder(ServletContext servletContext, ObjectWrapper objectWrapper) {
        this.servletContext = servletContext;
        this.objectWrapper = objectWrapper;
    }

    public TaglibFactoryBuilder addMetaInfTldSource(MetaInfTldSource metaInfTldSource) {
        metaInfTldSources.add(metaInfTldSource);
        return this;
    }

    public TaglibFactoryBuilder addAllMetaInfTldSources(List<MetaInfTldSource> metaInfTldSources) {
        this.metaInfTldSources.addAll(metaInfTldSources);
        return this;
    }

    public TaglibFactoryBuilder addMetaInfTldLocation(String metaInfTldLocation) throws ParseException {
        return addMetaInfTldSource(parseMetaInfTldLocation(metaInfTldLocation));
    }

    public TaglibFactoryBuilder addMetaInfTldLocations(List<String> metaInfTldLocations) throws ParseException {
        return addAllMetaInfTldSources(parseMetaInfTldLocations(metaInfTldLocations));
    }

    public TaglibFactoryBuilder addJettyMetaInfTldJarPattern(Pattern pattern) {
        return addMetaInfTldSource(new ClasspathMetaInfTldSource(pattern));
    }

    public TaglibFactoryBuilder addAllJettyMetaInfTldJarPatterns(List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            addJettyMetaInfTldJarPattern(pattern);
        }

        return this;
    }

    public TaglibFactoryBuilder addClasspathTld(String classpathTld) {
        classPathTlds.add(classpathTld);
        return this;
    }

    public TaglibFactoryBuilder addAllClasspathTlds(List<String> classpathTlds) {
        classPathTlds.addAll(classpathTlds);
        return this;
    }

    public TaglibFactory build() {
        TaglibFactory taglibFactory = new TaglibFactory(servletContext);
        taglibFactory.setObjectWrapper(objectWrapper);
        taglibFactory.setMetaInfTldSources(metaInfTldSources);
        taglibFactory.setClasspathTlds(classPathTlds);
        return taglibFactory;
    }

    public static MetaInfTldSource parseMetaInfTldLocation(String value) throws ParseException {
        MetaInfTldSource metaInfTldSource;

        if (value.equals(FreemarkerServlet.META_INF_TLD_LOCATION_WEB_INF_PER_LIB_JARS)) {
            metaInfTldSource = WebInfPerLibJarMetaInfTldSource.INSTANCE;
        } else if (value.startsWith(FreemarkerServlet.META_INF_TLD_LOCATION_CLASSPATH)) {
            String itemRightSide = value.substring(FreemarkerServlet.META_INF_TLD_LOCATION_CLASSPATH.length())
                    .trim();

            if (itemRightSide.length() == 0) {
                metaInfTldSource = new ClasspathMetaInfTldSource(Pattern.compile(".*", Pattern.DOTALL));
            } else if (itemRightSide.startsWith(":")) {
                final String regexpStr = itemRightSide.substring(1).trim();
                if (regexpStr.length() == 0) {
                    throw new ParseException("Empty regular expression after \""
                            + FreemarkerServlet.META_INF_TLD_LOCATION_CLASSPATH + ":\"", -1);
                }
                metaInfTldSource = new ClasspathMetaInfTldSource(Pattern.compile(regexpStr));
            } else {
                throw new ParseException("Invalid \"" + FreemarkerServlet.META_INF_TLD_LOCATION_CLASSPATH
                        + "\" value syntax: " + value, -1);
            }
        } else if (value.startsWith(FreemarkerServlet.META_INF_TLD_LOCATION_CLEAR)) {
            metaInfTldSource = ClearMetaInfTldSource.INSTANCE;
        } else {
            throw new ParseException("Item has no recognized source type prefix: " + value, -1);
        }

        return metaInfTldSource;
    }

    public static List<MetaInfTldSource> parseMetaInfTldLocations(List<String> values) throws ParseException {
        List<MetaInfTldSource> metaInfTldSources = null;

        if (values != null) {
            for (String value : values) {
                final MetaInfTldSource metaInfTldSource = parseMetaInfTldLocation(value);

                if (metaInfTldSources == null) {
                    metaInfTldSources = new ArrayList();
                }

                metaInfTldSources.add(metaInfTldSource);
            }
        }

        if (metaInfTldSources == null) {
            metaInfTldSources = Collections.emptyList();
        }

        return metaInfTldSources;
    }

}
