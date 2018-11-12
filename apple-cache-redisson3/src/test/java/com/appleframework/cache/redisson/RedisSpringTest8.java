package com.appleframework.cache.redisson;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:config/test-apple-cache-spring2.xml" })
public class RedisSpringTest8 {

	@Resource
	private TestService testService;

	@Test
	public void testAddOpinion1() {
		try {
			for (int i = 1; i < 100000; i++) {
				try {
					System.out.println(testService.getCache("xusm"));
					Thread.sleep(1000);
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
			}
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
