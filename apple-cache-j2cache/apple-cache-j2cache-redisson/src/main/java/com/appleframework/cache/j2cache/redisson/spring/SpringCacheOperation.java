package com.appleframework.cache.j2cache.redisson.spring;

import java.util.Map;

import org.apache.log4j.Logger;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import com.appleframework.cache.core.replicator.Command;
import com.appleframework.cache.core.replicator.Command.CommandType;
import com.appleframework.cache.core.spring.BaseCacheOperation;
import com.appleframework.cache.j2cache.redisson.utils.Contants;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class SpringCacheOperation implements BaseCacheOperation {

	private static Logger logger = Logger.getLogger(SpringCacheOperation.class);

	private String name;
	private int expire = 0;
	private RedissonClient redisson;
	private CacheManager ehcacheManager;
	private RTopic topic;
	
	public SpringCacheOperation(CacheManager ehcacheManager, RedissonClient redisson, String name) {
		this.name = name;
		this.redisson = redisson;
		this.ehcacheManager = ehcacheManager;
		init();
	}
	
	public SpringCacheOperation(CacheManager ehcacheManager, RedissonClient redisson, String name, int expire) {
		this.name = name;
		this.expire = expire;
		this.redisson = redisson;
		this.ehcacheManager = ehcacheManager;
		init();
	}
	
	public Map<String, Object> getRedisCache() {
		return redisson.getMap(name);
	}
	
	public Cache getEhCache() {
		Cache cache = ehcacheManager.getCache(name);
		if(null == cache) {
			ehcacheManager.addCache(name);
			return ehcacheManager.getCache(name);
		}
		else {
			return cache;
		}
	}
	
	public void init() {
		topic = redisson.getTopic(Contants.TOPIC_PREFIX_KEY + name);
		topic.addListener(Command.class, new MessageListener<Command>() {
		    @Override
			public void onMessage(CharSequence channel, Command message) {
		    	Object key = message.getKey();
		    	Cache cache = getEhCache();
		    	if(null != cache) {
			    	if(message.getType().equals(CommandType.PUT)) {
			    		cache.remove(key);
			    	}
			    	else if(message.getType().equals(CommandType.DELETE)) {
			    		cache.remove(key);
			    	}
			    	else if(message.getType().equals(CommandType.CLEAR)) {
			    		cache.removeAll();
			    	}
			    	else {
			    		logger.error("ERROR OPERATE TYPE !!!");
			    	}
		    	}
			}
		});
	}

	public Object get(String key) {
		Object value = null;
		try {
			Element element = getEhCache().get(key);
			if(null == element) {
				value = getRedisCache().get(key);
				if(null != value) {
					if(expire > 0)
						getEhCache().put(new Element(key, value, expire, expire));
					else
						getEhCache().put(new Element(key, value));
				}
			}
			else {
				value = element.getObjectValue();
			}
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
		return value;
	}

	public void put(String key, Object value) {
		if (value == null)
			this.delete(key);
		try {
			getRedisCache().put(key, value);
			publish(key, CommandType.PUT);
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
	}

	public void clear() {
		try {
			getRedisCache().clear();
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
		publish(null, CommandType.CLEAR);
	}

	public void delete(String key) {
		try {
			getRedisCache().remove(key);
			publish(key, CommandType.DELETE);
		} catch (Exception e) {
			logger.warn("cache error", e);
		}
	}

	public int getExpire() {
		return expire;
	}
	
	private void publish(Object key, CommandType commandType) {        
		Command object = new Command();
		object.setKey(key);
		object.setType(commandType);
		try {
			topic.publish(object);
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}	
}
