package com.appleframework.cache.redisson.spring;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.redisson.Redisson;
import org.redisson.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

import com.appleframework.cache.core.config.CacheConfig;

public class SpringCacheManager extends AbstractCacheManager {

	private ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<String, Cache>();
	private Map<String, Integer> expireMap = new HashMap<String, Integer>();

	private RedissonClient redisson;

	public SpringCacheManager() {
	}

	@Override
	protected Collection<? extends Cache> loadCaches() {
		Collection<Cache> values = cacheMap.values();
		return values;
	}

	@Override
	public Cache getCache(String name) {
		Cache cache = cacheMap.get(name);
		if (cache == null) {
			Integer expire = expireMap.get(name);
			if (expire == null) {
				expire = 0;
				expireMap.put(name, expire);
			}
			cache = new SpringCache(redisson, name, expire.intValue());
			cacheMap.put(name, cache);
		}
		return cache;
	}

	public void setRedisson(Redisson redisson) {
		this.redisson = redisson;
	}

	public void setExpireConfig(Map<String, Integer> expireConfig) {
		this.expireMap = expireConfig;
	}

	public void setCacheObject(Boolean isCacheObject) {
		CacheConfig.setCacheObject(isCacheObject);
	}
	
	public void setCacheEnable(Boolean isCacheEnable) {
		CacheConfig.setCacheEnable(isCacheEnable);
	}
}