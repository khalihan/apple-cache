package com.appleframework.cache.redis.spring;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import com.appleframework.cache.core.config.SpringCacheConfig;
import com.appleframework.cache.core.spring.BaseCacheOperation;

public class SpringCacheOperationBucket implements BaseCacheOperation {

	private static Logger logger = LoggerFactory.getLogger(SpringCacheOperationBucket.class);

	private String name;
	private int expireTime = 0;
	private RedisTemplate<String, Object> redisTemplate;

	public SpringCacheOperationBucket(String name, int expireTime, RedisTemplate<String, Object> redisTemplate) {
		this.name = name;
		this.expireTime = expireTime;
		this.redisTemplate = redisTemplate;
	}
	
	private String getCacheKey(String key) {
		return SpringCacheConfig.getCacheKeyPrefix() + name + ":" + key;
	}
	
	public Object get(String key) {
		Object value = null;
		try {
			return redisTemplate.opsForValue().get(getCacheKey(key));
		} catch (Exception e) {
			logger.warn("Cache Error : ", e);
		}
		return value;
	}
	
	public void put(String key, Object value) {
		if (value == null)
			this.delete(key);
		try {
			if (expireTime > 0) {
				redisTemplate.opsForValue().set(key, value);
			}
			else {
				redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
			}
		} catch (Exception e) {
			logger.warn("Cache Error : ", e);
		}
	}

	public void clear() {
		try {
			String pattern = getCacheKey("*");
			Set<String> keys = redisTemplate.keys(pattern);
			redisTemplate.delete(keys);
		} catch (Exception e) {
			logger.warn("Cache Error : ", e);
		}
	}

	public void delete(String key) {
		try {
			redisTemplate.delete(getCacheKey(key));
		} catch (Exception e) {
			logger.warn("Cache Error : ", e);
		}
	}

	public int getExpireTime() {
		return expireTime;
	}
	
}
