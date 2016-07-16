package com.appleframework.cache.redis;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.appleframework.cache.core.CacheManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:config/apple-cache-redis-manager3.xml" })
public class RedisSpringTest7 {

	@Resource
	private CacheManager cacheManager;

	@Test
	public void testAddOpinion1() {
		try {
			String key = "123456";
			cacheManager.set(key, "123"); // tom�ĺ����б�
			System.out.println(cacheManager.get(key));
			System.in.read();
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
