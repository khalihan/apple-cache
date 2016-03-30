package com.appleframework.cache.redis;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.appleframework.cache.core.CacheException;
import com.appleframework.cache.core.CacheManager;
import com.appleframework.cache.core.utils.SerializeUtility;


public class RedisCacheManager implements CacheManager {

	private static Logger logger = Logger.getLogger(RedisCacheManager.class);
	
	private JedisPool jedisPool;

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}
	
	@SuppressWarnings("deprecation")
	public void clear() throws CacheException {
		Jedis jedis = jedisPool.getResource();
		try {
			jedis.flushDB();
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			jedisPool.returnResource(jedis);
		}
	}

	@SuppressWarnings("deprecation")
	public Object get(String key) throws CacheException {
		Jedis jedis = jedisPool.getResource();
		try {
			byte[] value = jedis.get(key.getBytes());
			return SerializeUtility.unserialize(value);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new CacheException(e.getMessage());
		} finally {
			jedisPool.returnResource(jedis);
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public <T> T get(String key, Class<T> clazz) throws CacheException {
		Jedis jedis = jedisPool.getResource();
		try {
			byte[] value = jedis.get(key.getBytes());
		    return (T)SerializeUtility.unserialize(value);
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new CacheException(e.getMessage());
		} finally {
			jedisPool.returnResource(jedis);
		}
	}

	@SuppressWarnings("deprecation")
	public boolean remove(String key) throws CacheException {
		Jedis jedis = jedisPool.getResource();
		try {
			return jedis.del(key.getBytes())>0;
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			jedisPool.returnResource(jedis);
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	public void set(String key, Object obj) throws CacheException {
		Jedis jedis = jedisPool.getResource();
		if (null != obj) {
			try {
				String o = jedis.set(key.getBytes(), SerializeUtility.serialize(obj));
				logger.info(o);
			} catch (Exception e) {
				logger.error(e.getMessage());
			} finally {
				jedisPool.returnResource(jedis);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void set(String key, Object obj, int expireTime) throws CacheException {
		Jedis jedis = jedisPool.getResource();
		if (null != obj) {
			try {
				jedis.set(key.getBytes(), SerializeUtility.serialize(obj));
				jedis.expire(key.getBytes(), expireTime);
			} catch (Exception e) {
				logger.error(e.getMessage());
			} finally {
				jedisPool.returnResource(jedis);
			}
		}
	}

}