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

package freemarker.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * A cache storage that implements a two-level Most Recently Used cache. In the
 * first level, items are strongly referenced up to the specified maximum. When
 * the maximum is exceeded, the least recently used item is moved into the  
 * second level cache, where they are softly referenced, up to another 
 * specified maximum. When the second level maximum is also exceeded, the least 
 * recently used item is discarded altogether. This cache storage is a 
 * generalization of both {@link StrongCacheStorage} and 
 * {@link SoftCacheStorage} - the effect of both of them can be achieved by 
 * setting one maximum to zero and the other to the largest positive integer. 
 * On the other hand, if you wish to use this storage in a strong-only mode, or
 * in a soft-only mode, you might consider using {@link StrongCacheStorage} or
 * {@link SoftCacheStorage} instead, as they can be used by 
 * {@link TemplateCache} concurrently without any synchronization on a 5.0 or 
 * later JRE.
 *  
 * <p>This class is <em>NOT</em> thread-safe. If it's accessed from multiple
 * threads concurrently, proper synchronization must be provided by the callers.
 * Note that {@link TemplateCache}, the natural user of this class provides the
 * necessary synchronizations when it uses the class.
 * Also you might consider whether you need this sort of a mixed storage at all
 * in your solution, as in most cases SoftCacheStorage can also be sufficient. 
 * SoftCacheStorage will use Java soft references, and they already use access 
 * timestamps internally to bias the garbage collector against clearing 
 * recently used references, so you can get reasonably good (and 
 * memory-sensitive) most-recently-used caching through 
 * {@link SoftCacheStorage} as well.
 *
 * @see freemarker.template.Configuration#setCacheStorage(CacheStorage)
 */
public class MruCacheStorage implements CacheStorageWithGetSize
{
    private final MruEntry strongHead = new MruEntry();
    private final MruEntry softHead = new MruEntry();
    {
        softHead.linkAfter(strongHead);
    }
    private final Map map = new HashMap();
    private final ReferenceQueue refQueue = new ReferenceQueue();
    private final int strongSizeLimit;
    private final int softSizeLimit;
    private int strongSize = 0;
    private int softSize = 0;
    
    /**
     * Creates a new MRU cache storage with specified maximum cache sizes. Each
     * cache size can vary between 0 and {@link Integer#MAX_VALUE}.
     * @param strongSizeLimit the maximum number of strongly referenced templates; when exceeded, the entry used
     *          the least recently will be moved into the soft cache.
     * @param softSizeLimit the maximum number of softly referenced templates; when exceeded, the entry used
     *          the least recently will be discarded.
     */
    public MruCacheStorage(int strongSizeLimit, int softSizeLimit) {
        if(strongSizeLimit < 0) throw new IllegalArgumentException("strongSizeLimit < 0");
        if(softSizeLimit < 0) throw new IllegalArgumentException("softSizeLimit < 0");
        this.strongSizeLimit = strongSizeLimit;
        this.softSizeLimit = softSizeLimit;
    }
    
    public Object get(Object key) {
        removeClearedReferences();
        MruEntry entry = (MruEntry)map.get(key);
        if(entry == null) {
            return null;
        }
        relinkEntryAfterStrongHead(entry, null);
        Object value = entry.getValue();
        if(value instanceof MruReference) {
            // This can only happen with strongSizeLimit == 0
            return ((MruReference)value).get();
        }
        return value;
    }

    public void put(Object key, Object value) {
        removeClearedReferences();
        MruEntry entry = (MruEntry)map.get(key);
        if(entry == null) {
            entry = new MruEntry(key, value);
            map.put(key, entry);
            linkAfterStrongHead(entry);
        }
        else {
            relinkEntryAfterStrongHead(entry, value);
        }
        
    }

    public void remove(Object key) {
        removeClearedReferences();
        removeInternal(key);
    }

    private void removeInternal(Object key) {
        MruEntry entry = (MruEntry)map.remove(key);
        if(entry != null) {
            unlinkEntryAndInspectIfSoft(entry);
        }
    }

    public void clear() {
        strongHead.makeHead();
        softHead.linkAfter(strongHead);
        map.clear();
        strongSize = softSize = 0;
        // Quick refQueue processing
        while(refQueue.poll() != null);
    }

    private void relinkEntryAfterStrongHead(MruEntry entry, Object newValue) {
        if(unlinkEntryAndInspectIfSoft(entry) && newValue == null) {
            // Turn soft reference into strong reference, unless is was cleared
            MruReference mref = (MruReference)entry.getValue();
            Object strongValue = mref.get();
            if (strongValue != null) {
                entry.setValue(strongValue);
                linkAfterStrongHead(entry);
            } else {
                map.remove(mref.getKey());
            }
        } else {
            if (newValue != null) {
                entry.setValue(newValue);
            }
            linkAfterStrongHead(entry);
        }
    }

    private void linkAfterStrongHead(MruEntry entry) {
        entry.linkAfter(strongHead);
        if(strongSize == strongSizeLimit) {
            // softHead.previous is LRU strong entry
            MruEntry lruStrong = softHead.getPrevious();
            // Attila: This is equaivalent to strongSizeLimit != 0
            // DD: But entry.linkAfter(strongHead) was just executed above, so
            //     lruStrong != strongHead is true even if strongSizeLimit == 0.
            if(lruStrong != strongHead) {
                lruStrong.unlink();
                if(softSizeLimit > 0) {
                    lruStrong.linkAfter(softHead);
                    lruStrong.setValue(new MruReference(lruStrong, refQueue));
                    if(softSize == softSizeLimit) {
                        // List is circular, so strongHead.previous is LRU soft entry
                        MruEntry lruSoft = strongHead.getPrevious();
                        lruSoft.unlink();
                        map.remove(lruSoft.getKey());
                    }
                    else {
                        ++softSize;
                    }
                }
                else {
                    map.remove(lruStrong.getKey());
                }
            }
        }
        else {
            ++strongSize;
        }
    }

    private boolean unlinkEntryAndInspectIfSoft(MruEntry entry) {
        entry.unlink();
        if(entry.getValue() instanceof MruReference) {
            --softSize;
            return true;
        }
        else {
            --strongSize;
            return false;
        }
    }
    
    private void removeClearedReferences() {
        for(;;) {
            MruReference ref = (MruReference)refQueue.poll();
            if(ref == null) {
                break;
            }
            removeInternal(ref.getKey());
        }
    }
    
    /**
     * Returns the configured upper limit of the number of strong cache entries.
     *  
     * @since 2.3.21
     */
    public int getStrongSizeLimit() {
        return strongSizeLimit;
    }

    /**
     * Returns the configured upper limit of the number of soft cache entries.
     * 
     * @since 2.3.21
     */
    public int getSoftSizeLimit() {
        return softSizeLimit;
    }

    /**
     * Returns the <em>current</em> number of strong cache entries.
     *  
     * @see #getStrongSizeLimit()
     * @since 2.3.21
     */
    public int getStrongSize() {
        return strongSize;
    }

    /**
     * Returns a close approximation of the <em>current</em> number of soft cache entries.
     * 
     * @see #getSoftSizeLimit()
     * @since 2.3.21
     */
    public int getSoftSize() {
        removeClearedReferences();
        return softSize;
    }
    
    /**
     * Returns a close approximation of the current number of cache entries.
     * 
     * @see #getStrongSize()
     * @see #getSoftSize()
     * @since 2.3.21
     */
    public int getSize() {
        return getSoftSize() + getStrongSize();
    }

    private static final class MruEntry
    {
        private MruEntry prev;
        private MruEntry next;
        private final Object key;
        private Object value;
        
        /**
         * Used solely to construct the head element
         */
        MruEntry()
        {
            makeHead();
            key = value = null;
        }
        
        MruEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
        
        Object getKey() {
            return key;
        }
        
        Object getValue() {
            return value;
        }
        
        void setValue(Object value) {
            this.value = value;
        }

        MruEntry getPrevious() {
            return prev;
        }
        
        void linkAfter(MruEntry entry) {
            next = entry.next;
            entry.next = this;
            prev = entry;
            next.prev = this;
        }
        
        void unlink() {
            next.prev = prev;
            prev.next = next;
            prev = null;
            next = null;
        }
        
        void makeHead() {
            prev = next = this;
        }
    }
    
    private static class MruReference extends SoftReference
    {
        private final Object key;
        
        MruReference(MruEntry entry, ReferenceQueue queue) {
            super(entry.getValue(), queue);
            this.key = entry.getKey();
        }
        
        Object getKey() {
            return key;
        }
    }
    
    
}