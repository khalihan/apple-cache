package com.appleframework.cache.j2cache.jedis.replicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.appleframework.cache.core.replicator.Command;
import com.appleframework.cache.core.replicator.CommandReceiver;
import com.appleframework.cache.jedis.factory.PoolFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class CommandRedisTopicReceiver implements CommandReceiver {
	
	protected final static Logger logger = LoggerFactory.getLogger(CommandRedisTopicReceiver.class);
	
	private String name = "J2_CACHE_MANAGER";
	
	private PoolFactory poolFactory;
		
	private CommandRedisTopicProcesser commandProcesser;
	
	private Thread threadSubscribe;
		
	public void init() {
		threadSubscribe = new Thread(new Runnable() {
			@Override
			public void run() {
				JedisPool jedisPool = poolFactory.getReadPool();
				Jedis jedis = jedisPool.getResource();
				try {
					logger.warn("The subscribe channel is " + name);
					jedis.subscribe(commandProcesser, name.getBytes());
				} catch (Exception e) {
					logger.error("Subscribing failed.", e);
				}
			}
		});
		threadSubscribe.start();
	}

	@Override
	public void onMessage(Command command) {
		commandProcesser.onProcess(command);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPoolFactory(PoolFactory poolFactory) {
		this.poolFactory = poolFactory;
	}

	public void setCommandProcesser(CommandRedisTopicProcesser commandProcesser) {
		this.commandProcesser = commandProcesser;
	}
}
