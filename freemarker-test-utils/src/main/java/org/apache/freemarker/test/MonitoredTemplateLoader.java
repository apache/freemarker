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
package org.apache.freemarker.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.freemarker.core.templateresolver.TemplateLoader;
import org.apache.freemarker.core.templateresolver.TemplateLoaderSession;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResult;
import org.apache.freemarker.core.templateresolver.TemplateLoadingResultStatus;
import org.apache.freemarker.core.templateresolver.TemplateLoadingSource;

import com.google.common.base.Objects;

public class MonitoredTemplateLoader implements TemplateLoader {

    private final List<AbstractTemplateLoader2Event> events
            = Collections.synchronizedList(new ArrayList<AbstractTemplateLoader2Event>());
    
    private Map<String, StoredTemplate> templates
            = new ConcurrentHashMap<>();

    @Override
    public TemplateLoaderSession createSession() {
        events.add(CreateSessionEvent.INSTANCE);
        return new MonitoredTemplateLoader2Session();
    }

    @Override
    public TemplateLoadingResult load(String name, TemplateLoadingSource ifSourceDiffersFrom, Serializable ifVersionDiffersFrom,
            TemplateLoaderSession session)
            throws IOException {
        TemplateLoadingResult result = loadInner(name, ifSourceDiffersFrom, ifVersionDiffersFrom, session);
        events.add(new LoadEvent(name, result.getStatus()));
        return result;
    }

    private TemplateLoadingResult loadInner(String name, TemplateLoadingSource ifSourceDiffersFrom,
            Serializable ifVersionDiffersFrom, TemplateLoaderSession session) throws UnsupportedEncodingException {
        if (session.isClosed()) {
            throw new IllegalArgumentException("Template loader session is already closed.");
        }
        
        StoredTemplate storedTemplate = templates.get(name);
        if (storedTemplate == null) {
            return TemplateLoadingResult.NOT_FOUND;
        }
        
        TemplateLoadingSource source = storedTemplate.getSource();
        Serializable version = storedTemplate.getVersion();
        if (ifSourceDiffersFrom != null
                && ifSourceDiffersFrom.equals(source)
                && Objects.equal(ifVersionDiffersFrom, version)) {
            return TemplateLoadingResult.NOT_MODIFIED;
        }
        
        String content = storedTemplate.getContent();
        return storedTemplate.inputStreamEncoding == null
                ? new TemplateLoadingResult(source, version, new StringReader(content), null)
                : new TemplateLoadingResult(source, version,
                        new ByteArrayInputStream(content.getBytes(storedTemplate.inputStreamEncoding)), null);
    }
    
    @Override
    public void resetState() {
        events.add(ResetStateEvent.INSTANCE);
    }

    public StoredTemplate putTextTemplate(String name, String content) {
        return putTextTemplate(name, "v1", content);
    }
    
    public StoredTemplate putTextTemplate(String name, Serializable version, String content) {
        StoredTemplate storedTemplate = new StoredTemplate(name, version, content, null);
        templates.put(name, storedTemplate);
        return storedTemplate;
    }

    public StoredTemplate putBinaryTemplate(String name, String content) {
        return putBinaryTemplate(name, content, "v1");
    }
    
    public StoredTemplate putBinaryTemplate(String name, String content, Serializable version) {
        return putBinaryTemplate(name, content, null, version);
    }
    
    public StoredTemplate putBinaryTemplate(String name, String content,
            Charset inputStreamEncoding, Serializable version) {
        StoredTemplate storedTemplate = new StoredTemplate(name, version, content,
                inputStreamEncoding != null ? inputStreamEncoding : StandardCharsets.UTF_8);
        templates.put(name, storedTemplate);
        return storedTemplate;
    }

    public List<AbstractTemplateLoader2Event> getEvents() {
        return events;
    }

    /**
     * Gets a filtered event list.
     */
    @SuppressWarnings("unchecked")
    public <E extends AbstractTemplateLoader2Event> List<E> getEvents(Class<E> eventClass) {
        List<E> result = new ArrayList<>();
        for (AbstractTemplateLoader2Event event : events) {
            if (eventClass.isInstance(event)) {
                result.add((E) event);
            }
        }
        return result;
    }
    
    /**
     * Extract from the {@link #getEvents()} the template names for which {@link TemplateLoader#load} was called.
     */
    public List<String> getLoadNames() {
        List<String> result = new ArrayList<>();
        for (AbstractTemplateLoader2Event event : events) {
            if (event instanceof LoadEvent) {
                result.add(((LoadEvent) event).name);
            }
        }
        return result;
    }

    public void clearEvents() {
        events.clear();
    }

    @SuppressWarnings("serial")
    public static class StoredTemplate implements TemplateLoadingSource {
        private String name;
        private Serializable version;
        private String content;
        private Charset inputStreamEncoding; 
        
        private StoredTemplate(String name, Serializable version, String content, Charset inputStreamEncoding) {
            this.name = name;
            this.version = version;
            this.content = content;
            this.inputStreamEncoding = inputStreamEncoding;
        }
    
        public String getName() {
            return name;
        }
    
        public void setName(String name) {
            this.name = name;
        }
    
        public TemplateLoadingSource getSource() {
            return this;
        }
    
        public Serializable getVersion() {
            return version;
        }
    
        public void setVersion(Serializable version) {
            this.version = version;
        }
    
        public String getContent() {
            return content;
        }
    
        public void setContent(String content) {
            this.content = content;
        }
    
        public Charset getInputStreamEncoding() {
            return inputStreamEncoding;
        }
    
        public void setInputStreamEncoding(Charset inputStreamEncoding) {
            this.inputStreamEncoding = inputStreamEncoding;
        }
        
    }

    public abstract static class AbstractTemplateLoader2Event {
        // empty
    }

    public static class LoadEvent extends AbstractTemplateLoader2Event {
        private final String name;
        private final TemplateLoadingResultStatus resultStatus;
        
        public LoadEvent(String name, TemplateLoadingResultStatus resultStatus) {
            this.name = name;
            this.resultStatus = resultStatus;
        }
    
        public String getName() {
            return name;
        }
        
        public TemplateLoadingResultStatus getResultStatus() {
            return resultStatus;
        }
    
        @Override
        public String toString() {
            return "LoadEvent [name=" + name + ", resultStatus=" + resultStatus + "]";
        }
    
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((resultStatus == null) ? 0 : resultStatus.hashCode());
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
            LoadEvent other = (LoadEvent) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return resultStatus == other.resultStatus;
        }
        
    }

    public static class ResetStateEvent extends AbstractTemplateLoader2Event {
    
        public static ResetStateEvent INSTANCE = new ResetStateEvent();
        
        private ResetStateEvent() {
            //
        }
        
        @Override
        public String toString() {
            return "ResetStateEvent []";
        }
        
    }

    public static class CreateSessionEvent extends AbstractTemplateLoader2Event {
        
        public static CreateSessionEvent INSTANCE = new CreateSessionEvent();
        
        private CreateSessionEvent() {
            //
        }

        @Override
        public String toString() {
            return "CreateSessionEvent []";
        }
        
    }

    public static class CloseSessionEvent extends AbstractTemplateLoader2Event {

        public static CloseSessionEvent INSTANCE = new CloseSessionEvent();
        
        private CloseSessionEvent() {
            //
        }
        
        @Override
        public String toString() {
            return "CloseSessionEvent []";
        }
        
    }

    public class MonitoredTemplateLoader2Session implements TemplateLoaderSession {
        
        private boolean closed;

        @Override
        public void close() {
            closed = true;
            events.add(CloseSessionEvent.INSTANCE);
        }

        @Override
        public boolean isClosed() {
            return closed;
        }
        
    }
    
}
