package com.appleframework.cache.codis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.appleframework.cache.core.CacheException;
import com.appleframework.cache.core.CacheManager;
import com.appleframework.cache.core.utils.SerializeUtility;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

@SuppressWarnings("unchecked")
@Component
public class CodisBucketCacheManager implements CacheManager {

	private static Logger logger = Logger.getLogger(CodisBucketCacheManager.class);
	
	@Resource
	private CodisResourcePool codisResourcePool;
	
	public CodisResourcePool getCodisResourcePool() {
		return codisResourcePool;
	}

	public void setCodisResourcePool(CodisResourcePool codisResourcePool) {
		this.codisResourcePool = codisResourcePool;
	}

	public void clear() throws CacheException {
		return;
	}

	public Object get(String key) throws CacheException {
		try (Jedis jedis = codisResourcePool.getResource()) {
			byte[] value = jedis.get(key.getBytes());
	     	return SerializeUtility.unserialize(value);
		}
	}

	@Override
	public <T> T get(String key, Class<T> clazz) throws CacheException {
		try (Jedis jedis = codisResourcePool.getResource()) {
			byte[] value = jedis.get(key.getBytes());
	     	return (T)SerializeUtility.unserialize(value);
		}
	}

	public boolean remove(String key) throws CacheException {
		try (Jedis jedis = codisResourcePool.getResource()) {
			return jedis.del(key.getBytes())>0;
		}
	}

	public void set(String key, Object obj) throws CacheException {
		try (Jedis jedis = codisResourcePool.getResource()) {
			String o = jedis.set(key.getBytes(), SerializeUtility.serialize(obj));
			logger.info(o);
		}
	}

	public void set(String key, Object obj, int expireTime) throws CacheException {
		try (Jedis jedis = codisResourcePool.getResource()) {
			jedis.set(key.getBytes(), SerializeUtility.serialize(obj));
			jedis.expire(key.getBytes(), expireTime);
		}
	}
	
	public void expire(String key, int expireTime) throws CacheException {
		try (Jedis jedis = codisResourcePool.getResource()) {
			jedis.expire(key.getBytes(), expireTime);
		}
	}

	@Override
	public List<Object> getList(List<String> keyList) throws CacheException {
		return this.getList(keyList.toArray(new String[keyList.size()]));
	}

	@Override
	public List<Object> getList(String... keys) throws CacheException {
		List<Object> list = new ArrayList<Object>();
		try (Jedis jedis = codisResourcePool.getResource()) {
		    Map<String, Response<byte[]>> responses = new HashMap<String, Response<byte[]>>(keys.length);

			Pipeline pipeline = jedis.pipelined();
			for (String key : keys) {
				responses.put(key, pipeline.get(key.getBytes()));
			}
			pipeline.sync();
			
			for(String key : responses.keySet()) {
				Response<byte[]> response = responses.get(key);
				byte[] value = response.get();
				if(null != value) {
					list.add(SerializeUtility.unserialize(value));
				}
				else {
					list.add(null);
				}
			}
		}
		return list;
	}

	@Override
	public <T> List<T> getList(Class<T> clazz, List<String> keyList) throws CacheException {
		return this.getList(clazz, keyList.toArray(new String[keyList.size()]));
	}

	@Override
	public <T> List<T> getList(Class<T> clazz, String... keys) throws CacheException {
		List<T> list = new ArrayList<T>();
		try (Jedis jedis = codisResourcePool.getResource()) {
		    Map<String, Response<byte[]>> responses = new HashMap<String, Response<byte[]>>(keys.length);

			Pipeline pipeline = jedis.pipelined();
			for (String key : keys) {
				responses.put(key, pipeline.get(key.getBytes()));
			}
			pipeline.sync();
			
			for(String key : responses.keySet()) {
				Response<byte[]> response = responses.get(key);
				byte[] value = response.get();
				if(null != value) {
					list.add((T)SerializeUtility.unserialize(value));
				}
				else {
					list.add(null);
				}
			}
		}
		return list;
	}

	@Override
	public Map<String, Object> getMap(List<String> keyList) throws CacheException {
		return this.getMap(keyList.toArray(new String[keyList.size()]));
	}

	@Override
	public Map<String, Object> getMap(String... keys) throws CacheException {
		Map<String, Object> map = new HashMap<String, Object>();
		try (Jedis jedis = codisResourcePool.getResource()) {
		    Map<String, Response<byte[]>> responses = new HashMap<String, Response<byte[]>>(keys.length);

			Pipeline pipeline = jedis.pipelined();
			for (String key : keys) {
				responses.put(key, pipeline.get(key.getBytes()));
			}
			pipeline.sync();
			
			for(String key : responses.keySet()) {
				Response<byte[]> response = responses.get(key);
				byte[] value = response.get();
				if(null != value) {
					map.put(key, SerializeUtility.unserialize(value));
				}
				else {
					map.put(key, null);
				}
			}
		}
		return map;
	}

	@Override
	public <T> Map<String, T> getMap(Class<T> clazz, List<String> keyList) throws CacheException {
		return this.getMap(clazz, keyList.toArray(new String[keyList.size()]));
	}

	@Override
	public <T> Map<String, T> getMap(Class<T> clazz, String... keys) throws CacheException {
		Map<String, T> map = new HashMap<String, T>();
		try (Jedis jedis = codisResourcePool.getResource()) {
		    Map<String, Response<byte[]>> responses = new HashMap<String, Response<byte[]>>(keys.length);

			Pipeline pipeline = jedis.pipelined();
			for (String key : keys) {
				responses.put(key, pipeline.get(key.getBytes()));
			}
			pipeline.sync();
			
			for(String key : responses.keySet()) {
				Response<byte[]> response = responses.get(key);
				byte[] value = response.get();
				if(null != value) {
					map.put(key, (T)SerializeUtility.unserialize(value));
				}
				else {
					map.put(key, null);
				}
			}
		}
		return map;
	}
	
}