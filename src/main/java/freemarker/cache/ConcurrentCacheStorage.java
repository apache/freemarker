package freemarker.cache;

/**
 * An optional interface for cache storage that knows whether it can be 
 * concurrently accessible without synchronization.
 * @author Attila Szegedi
 */
public interface ConcurrentCacheStorage extends CacheStorage {
    
    /**
     * Returns true if this instance of cache storage is concurrently 
     * accessible from multiple threads without synchronization.
     * @return true if this instance of cache storage is concurrently 
     * accessible from multiple threads without synchronization.
     */
    public boolean isConcurrent();
}
