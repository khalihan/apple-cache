package com.appleframework.cache.ehcache3.config;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.appleframework.cache.ehcache.EhCacheExpiryUtil;
import com.appleframework.cache.ehcache.EhCacheManager;
import com.appleframework.cache.ehcache.config.EhCacheConfiguration;
import com.appleframework.cache.ehcache.config.EhCacheContants;
import com.appleframework.cache.ehcache.config.EhCacheProperties;
import com.appleframework.cache.ehcache.spring.SpringCacheManager;

@Configuration
@EnableConfigurationProperties(AppleCacheProperties.class)
@ConditionalOnProperty(prefix = AppleCacheProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class AppleCacheAutoConfiguration {

	@Autowired
	private AppleCacheProperties properties;
	
	private CacheManager ehCacheManager = null;
	
	@Bean
	@ConditionalOnMissingBean
	public EhCacheConfiguration configurationFactoryBean() {
		EhCacheConfiguration bean = new EhCacheConfiguration();
		bean.setDirectory(properties.getDirectory());
		bean.setProperties(properties.getCacheTemplate());
		return bean;
	}

	@Bean("ehCacheManager")
	public CacheManager ehCacheManagerFactory() throws Exception {
		if(null != ehCacheManager) {
			return ehCacheManager;
		}
		URL xmlUrl = getClass().getResource("/ehcache.xml");
		String directory = properties.getDirectory();
		String initName = properties.getInitName();
		if (null == xmlUrl) {
			Map<String, EhCacheProperties> cacheTemplate = properties.getCacheTemplate();
			EhCacheProperties property = null;
			if(null != cacheTemplate) {
				property = properties.getCacheTemplate().get(initName);
			}
			int heap = 10;
			int offheap = 100;
			int disk = 1000;
			boolean persistent = false;
			int ttl = 0;
			int tti = 0;
			ExpiryPolicy<Object, Object> expiryPolicy = null;
			if(null != property) {
				heap = property.getHeap();
				offheap = property.getOffheap();
				disk = property.getDisk();
				persistent = property.isPersistent();
				ttl = property.getTtl();
				tti = property.getTti();
			}
			else {
				heap = EhCacheContants.DEFAULT_HEAP;
				offheap = EhCacheContants.DEFAULT_OFFHEAP;
				disk = EhCacheContants.DEFAULT_DISK;
				persistent = EhCacheContants.DEFAULT_PERSISTENT;
				ttl = EhCacheContants.DEFAULT_TTL;
				tti = EhCacheContants.DEFAULT_TTI;
			}
			
			if(ttl > 0) {
				expiryPolicy = EhCacheExpiryUtil.instance("ttl", ttl);
			}
			
			if(tti > 0) {
				expiryPolicy = EhCacheExpiryUtil.instance("tti", tti);
			}
			
			if(tti <=0 && tti <=0 ) {
				expiryPolicy = EhCacheExpiryUtil.instance();
			}

			ehCacheManager = CacheManagerBuilder
					.newCacheManagerBuilder()
					.with(CacheManagerBuilder.persistence(new File(directory, "ehcacheData")))
					.withCache(initName,
							CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Serializable.class,
											ResourcePoolsBuilder.newResourcePoolsBuilder()
													.heap(heap, MemoryUnit.MB)
													.offheap(offheap, MemoryUnit.MB)
													.disk(disk, MemoryUnit.MB, persistent))
									.withExpiry(expiryPolicy))
					.build(true);
		} else {
			org.ehcache.config.Configuration xmlConfig = new XmlConfiguration(xmlUrl);
			ehCacheManager = CacheManagerBuilder.newCacheManager(xmlConfig);
			ehCacheManager.init();
		}
		return ehCacheManager;
	}

	
	@Bean
	@ConditionalOnMissingBean(SpringCacheManager.class)
	public SpringCacheManager springCacheManagerFactory() throws Exception {
		SpringCacheManager springCacheManager = new SpringCacheManager();
		if(null == ehCacheManager) {
			ehCacheManagerFactory();
		}
		springCacheManager.setEhcacheManager(ehCacheManager);
		Map<String, Integer> expireConfig = new HashMap<String, Integer>();
		Map<String, EhCacheProperties> cacheTemplate = properties.getCacheTemplate();
		if(null != cacheTemplate) {
			for (Map.Entry<String, EhCacheProperties> map : properties.getCacheTemplate().entrySet()) {
				String key = map.getKey();
				EhCacheProperties property = map.getValue();
				if(property.isSpringCache()) {
					expireConfig.put(key, property.getTti());
				}
			}	
			springCacheManager.setExpireConfig(expireConfig);
		}
		return springCacheManager;
	}
	
	@Bean("ehCache3Manager")
	@ConditionalOnBean(org.ehcache.CacheManager.class)
	public com.appleframework.cache.core.CacheManager appleCacheManagerFactory() throws Exception {
		EhCacheManager ehCache3Manager = new EhCacheManager();
		if(null == ehCacheManager) {
			ehCacheManagerFactory();
		}
		String name = properties.getInitName();
		ehCache3Manager.setName(name);
		ehCache3Manager.setEhcacheManager(ehCacheManager);
		return ehCache3Manager;
	}

}
