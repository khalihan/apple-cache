package com.appleframework.cache.caffeine;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:config/test-apple-cache-caffeine-spring2.xml"})
public class CaffeineCacheSpringTest2 {

	@Resource
	private TestService testService;
	    
	@Test
	public void testAddOpinion1() {
		try {
			for (int i = 1; i < 100; i++) {
				System.out.println(testService.getCache("xusm"));
				Thread.sleep(1000);
			}
			System.in.read();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	


}

