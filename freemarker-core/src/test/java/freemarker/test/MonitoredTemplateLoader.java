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

package freemarker.test;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import freemarker.cache.StringTemplateLoader;

public class MonitoredTemplateLoader extends StringTemplateLoader {
    
    List<AbstractTemplateLoaderEvent> events = Collections.synchronizedList(new ArrayList<AbstractTemplateLoaderEvent>());
    
    @Override
    public Object findTemplateSource(String name) {
        FindTemplateSourceEvent event = new FindTemplateSourceEvent(name, null);
        events.add(event);
        Object result = super.findTemplateSource(name);
        event.setFound(result != null);
        return result;
    }

    public void clearEvents() {
        events.clear();
    }
    
    @Override
    public void closeTemplateSource(Object templateSource) {
        events.add(new CloseTemplateSourceEvent(templateSource.toString()));
        super.closeTemplateSource(templateSource);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) {
        events.add(new GetReaderEvent(templateSource.toString()));
        return super.getReader(templateSource, encoding);
    }

    @Override
    public long getLastModified(Object templateSource) {
        events.add(new GetLastModifiedEvent(templateSource.toString()));
        return super.getLastModified(templateSource);
    }
    
    public List<AbstractTemplateLoaderEvent> getEvents() {
        return events;
    }
    
    public List<String> getNamesSearched() {
        ArrayList<String> result = new ArrayList<>();
        for (AbstractTemplateLoaderEvent event : events) {
            if (event instanceof FindTemplateSourceEvent) {
                result.add(((FindTemplateSourceEvent) event).getName());
            }
        }
        return result;
    }

    public static abstract class AbstractTemplateLoaderEvent {
        // empty
    }
    
    public static class FindTemplateSourceEvent extends AbstractTemplateLoaderEvent {
        private final String name;
        private Boolean found;
        
        public FindTemplateSourceEvent(String name, Boolean found) {
            this.name = name;
            this.found = found;
        }
        
        public void setFound(boolean found) {
            this.found = found;
        }
        
        public Boolean getFound() {
            return found;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((found == null) ? 0 : found.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
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
            FindTemplateSourceEvent other = (FindTemplateSourceEvent) obj;
            if (found == null) {
                if (other.found != null)
                    return false;
            } else if (!found.equals(other.found))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "FindTemplateSourceEvent [name=" + name + ", found=" + found + "]";
        }
        
    }

    public static class GetLastModifiedEvent extends AbstractTemplateLoaderEvent {
        private final String sourceName;
    
        public GetLastModifiedEvent(String sourceName) {
            this.sourceName = sourceName;
        }
    
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((sourceName == null) ? 0 : sourceName.hashCode());
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
            GetLastModifiedEvent other = (GetLastModifiedEvent) obj;
            if (sourceName == null) {
                if (other.sourceName != null)
                    return false;
            } else if (!sourceName.equals(other.sourceName))
                return false;
            return true;
        }
    
        @Override
        public String toString() {
            return "GetLastModifiedEvent [sourceName=" + sourceName + "]";
        }
        
    }
    
    public static class GetReaderEvent extends AbstractTemplateLoaderEvent {
        private final String sourceName;

        public GetReaderEvent(String sourceName) {
            this.sourceName = sourceName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((sourceName == null) ? 0 : sourceName.hashCode());
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
            GetReaderEvent other = (GetReaderEvent) obj;
            if (sourceName == null) {
                if (other.sourceName != null)
                    return false;
            } else if (!sourceName.equals(other.sourceName))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "GetReaderEvent [sourceName=" + sourceName + "]";
        }
        
    }

    public static class CloseTemplateSourceEvent extends AbstractTemplateLoaderEvent {
        private final String sourceName;

        public CloseTemplateSourceEvent(String sourceName) {
            this.sourceName = sourceName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((sourceName == null) ? 0 : sourceName.hashCode());
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
            CloseTemplateSourceEvent other = (CloseTemplateSourceEvent) obj;
            if (sourceName == null) {
                if (other.sourceName != null)
                    return false;
            } else if (!sourceName.equals(other.sourceName))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "CloseTemplateSourceEvent [sourceName=" + sourceName + "]";
        }
        
    }
    
}