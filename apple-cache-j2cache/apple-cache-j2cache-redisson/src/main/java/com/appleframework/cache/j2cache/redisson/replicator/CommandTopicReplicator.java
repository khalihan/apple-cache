package com.appleframework.cache.j2cache.redisson.replicator;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appleframework.cache.core.replicator.Command;
import com.appleframework.cache.core.replicator.CommandReplicator;
import com.appleframework.cache.j2cache.redisson.utils.Contants;

public class CommandTopicReplicator implements CommandReplicator {
	
	private static Logger logger = LoggerFactory.getLogger(CommandTopicReplicator.class);
	
	private String name = "J2_CACHE_MANAGER";

	private RedissonClient redisson;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setRedisson(RedissonClient redisson) {
		this.redisson = redisson;
	}
	
	private RTopic topic;
	
	public void init() {
		topic = redisson.getTopic(Contants.TOPIC_PREFIX_KEY + name);
	}

	public void replicate(Command command) {
		logger.warn("send command: " + command);
		if (null != command) {
			try {
				logger.warn("The publish channel is " + name);
				Long o = topic.publish(command);
				logger.info(o + "");
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
	}
}
