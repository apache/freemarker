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
package org.apache.freemarker.spring;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * A {@link TemplateLoader} that uses Spring Framework <code>Resource</code>s which are resolved by locations.
 */
public class SpringResourceTemplateLoader implements TemplateLoader, ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(SpringResourceTemplateLoader.class);

    /**
     * System template resource location prefix.
     */
    private static final String SYSTEM_TEMPLATE_RESOURCE_LOCATION_PREFIX = "classpath:org/apache/freemarker/spring/template/library/";

    /**
     * Default system template aliases map.
     */
    @SuppressWarnings("serial")
    private static final Map<String, String> DEFAULT_SYSTEM_TEMPLATE_ALIASES =
            Collections.unmodifiableMap(new HashMap<String, String>() {
                {
                    put("spring.ftl", SYSTEM_TEMPLATE_RESOURCE_LOCATION_PREFIX + "spring.ftl");
                }
            });

    /**
     * Base template resource location.
     * <P>
     * If this property is a non-null string, this is prepended to the template name internally when resolving
     * a resource.
     * </P>
     */
    private String baseLocation;

    /**
     * Spring Framework resource loader.
     */
    private ResourceLoader resourceLoader;

    /**
     * System Template Aliases Map
     */
    private Map<String, String> systemTemplateAliases = DEFAULT_SYSTEM_TEMPLATE_ALIASES;

    /**
     * Construct SpringResourceTemplateLoader.
     */
    public SpringResourceTemplateLoader() {
    }

    /**
     * Base resource location which can be prepended to the template name internally when resolving a resource.
     * @return
     */
    public String getBaseLocation() {
        return baseLocation;
    }

    /**
     * Set base resource location which can be prepended to the template name internally when resolving a resource.
     * @param baseLocation
     */
    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }

    /**
     * Return an immutable or unmodifiable system template aliases map, keyed by the alias and valued by the template
     * name.
     * @return an immutable or unmodifiable system template aliases map, keyed by the alias and valued by the template
     * name
     */
    public Map<String, String> getSystemTemplateAliases() {
        return systemTemplateAliases;
    }

    /**
     * Sets system template aliases map, keyed by the alias and valued by the template name
     * @param systemTemplateAliases system template aliases map, keyed by the alias and valued by the template
     * name
     */
    public void setSystemTemplateAliases(Map<String, String> systemTemplateAliases) {
        if (systemTemplateAliases == null) {
            this.systemTemplateAliases = Collections.emptyMap();
        } else {
            this.systemTemplateAliases = Collections.unmodifiableMap(new HashMap<>(systemTemplateAliases));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateLoaderSession createSession() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws IOException {
        if (resourceLoader == null) {
            throw new IllegalStateException("Spring Framework ResourceLoader was not set.");
        }

        String resourceLocation;

        if (systemTemplateAliases.containsKey(name)) {
            resourceLocation = systemTemplateAliases.get(name);
        } else {
            if (baseLocation == null) {
                resourceLocation = name;
            } else {
                if (baseLocation.endsWith("/")) {
                    resourceLocation = baseLocation + name;
                } else {
                    resourceLocation = baseLocation + "/" + name;
                }
            }
        }

        Resource resource = resourceLoader.getResource(resourceLocation);

        if (!resource.exists()) {
            return TemplateLoadingResult.NOT_FOUND;
        }

        ResourceTemplateLoadingSource source = new ResourceTemplateLoadingSource(resource);

        Long version = null;

        try {
            long lmd = resource.lastModified();
            if (lmd != -1) {
                version = lmd;
            }
        } catch (IOException e) {
            LOG.debug("The last modified timestamp of the resource at '{}' may not be resolved.", resourceLocation, e);
        }

        if (ifSourceDiffersFrom != null && ifSourceDiffersFrom.equals(source)
                && Objects.equals(ifVersionDiffersFrom, version)) {
            return TemplateLoadingResult.NOT_MODIFIED;
        }

        return new TemplateLoadingResult(source, version, resource.getInputStream(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetState() {
        // Does nothing
    }

    @SuppressWarnings("serial")
    private static class ResourceTemplateLoadingSource implements TemplateLoadingSource {

        private final Resource resource;

        ResourceTemplateLoadingSource(Resource resource) {
            this.resource = resource;
        }

        @Override
        public int hashCode() {
            return resource.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            return resource.equals(((ResourceTemplateLoadingSource) obj).resource);
        }

    }

}
