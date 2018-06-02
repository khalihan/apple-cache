package com.appleframework.cache.jedis;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

import redis.clients.jedis.BinaryJedisCluster;
import redis.clients.jedis.Client;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.JedisClusterInfoCache;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.PipelineBase;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.SafeEncoder;

/**
 * �ڼ�Ⱥģʽ���ṩ���������Ĺ��ܡ� <br/>
 * ���ڼ�Ⱥģʽ���ڽڵ�Ķ�̬���ɾ������client����ʵʱ��֪��ֻ����ִ������ʱ�ſ���֪����Ⱥ�����������
 * ��ˣ���ʵ�ֲ���֤һ���ɹ�����������������֮ǰ���� refreshCluster() �������»�ȡ��Ⱥ��Ϣ��<br />
 * Ӧ����Ҫ��֤���۳ɹ�����ʧ�ܶ������close() ������������ܻ����й¶��<br/>
 * ���ʧ����ҪӦ���Լ�ȥ���ԣ����ÿ������ִ�е�����������Ҫ���ơ���ֹʧ�ܺ����Ե��������ࡣ<br />
 * ��������˵���������ڼ�Ⱥ�������ȶ��������ڵ㲻�����Ƶ�����������ʹ�ã�������ʧ�ܻ��ж�Ӧ�����Բ��ԡ�<br />
 * 
 * 
 * @author youaremoon
 * @version
 * @since Ver 1.1
 */
public class JedisClusterPipeline extends PipelineBase implements Closeable {

	private static Logger logger = Logger.getLogger(JedisClusterPipeline.class);

    // �����ֶ�û�ж�Ӧ�Ļ�ȡ������ֻ�ܲ��÷�������
    // ��Ҳ����ȥ�̳�JedisCluster��JedisSlotBasedConnectionHandler���ṩ���ʽӿ�
    private static final Field FIELD_CONNECTION_HANDLER;
    private static final Field FIELD_CACHE; 
    static {
        FIELD_CONNECTION_HANDLER = getField(BinaryJedisCluster.class, "connectionHandler");
        FIELD_CACHE = getField(JedisClusterConnectionHandler.class, "cache");
    }

    private JedisSlotBasedConnectionHandler connectionHandler;
    private JedisClusterInfoCache clusterInfoCache;
    private Queue<Client> clients = new LinkedList<Client>();   // ����˳��洢ÿ�������Ӧ��Client
    private Map<JedisPool, Jedis> jedisMap = new HashMap<>();   // ���ڻ�������
    private boolean hasDataInBuf = false;   // �Ƿ��������ڻ�����

    /**
     * ����jedisClusterʵ�����ɶ�Ӧ��JedisClusterPipeline
     * @param 
     * @return
     */
    public static JedisClusterPipeline pipelined(JedisCluster jedisCluster) {
        JedisClusterPipeline pipeline = new JedisClusterPipeline();
        pipeline.setJedisCluster(jedisCluster);
        return pipeline;
    }

    public JedisClusterPipeline() {
    }

    public void setJedisCluster(JedisCluster jedis) {
        connectionHandler = getValue(jedis, FIELD_CONNECTION_HANDLER);
        clusterInfoCache = getValue(connectionHandler, FIELD_CACHE);
    }

    /**
     * ˢ�¼�Ⱥ��Ϣ������Ⱥ��Ϣ�������ʱ����
     * @param 
     * @return
     */
    public void refreshCluster() {
        connectionHandler.renewSlotCache();
    }

    /**
     * ͬ����ȡ��������. ��syncAndReturnAll()��ȣ�sync()ֻ��û�ж������������л�
     */
    public void sync() {
        innerSync(null);
    }

    /**
     * ͬ����ȡ�������� ��������˳�򷵻�һ���б�
     * 
     * @return ���������˳�򷵻����е�����
     */
    public List<Object> syncAndReturnAll() {
        List<Object> responseList = new ArrayList<Object>();
        innerSync(responseList);
        return responseList;
    }

    private void innerSync(List<Object> formatted) {
        HashSet<Client> clientSet = new HashSet<Client>();

        try {
            for (Client client : clients) {
                // ��sync()����ʱ��ʵ�ǲ���Ҫ����������ݵģ��������������get������������JedisMovedDataException�����Ĵ���Ӧ���ǲ�֪���ģ������Ҫ����get()����������
                // ��ʵ���Response��data���Կ���ֱ�ӻ�ȡ������ʡ���������ݵ�ʱ�䣬Ȼ������û���ṩ��Ӧ������Ҫ��ȡdata���Ծ͵��÷��䣬�����ٷ����ˣ����Ծ�������
                Object data = generateResponse(client.getOne()).get();
                if (null != formatted) {
                    formatted.add(data);
                }

                // size��ͬ˵�����е�client���Ѿ���ӣ��Ͳ����ٵ���add������
                if (clientSet.size() != jedisMap.size()) {
                    clientSet.add(client);
                }
            }
        } catch (JedisRedirectionException jre) {
            if (jre instanceof JedisMovedDataException) {
                // if MOVED redirection occurred, rebuilds cluster's slot cache,
                // recommended by Redis cluster specification
                refreshCluster();
            }

            throw jre;
        } finally {
            if (clientSet.size() != jedisMap.size()) {
                // ���л�û��ִ�й���clientҪ��ִ֤��(flush)����ֹ�Ż����ӳغ����������Ⱦ
                for (Jedis jedis : jedisMap.values()) {
                    if (clientSet.contains(jedis.getClient())) {
                        continue;
                    }

                    flushCachedData(jedis);
                }
            }

            hasDataInBuf = false;
            close();
        }
    }

    @Override
    public void close() {
        clean();
        clients.clear();
        for (Jedis jedis : jedisMap.values()) {
            if (hasDataInBuf) {
                flushCachedData(jedis);
            }
            jedis.close();
        }
        jedisMap.clear();
        hasDataInBuf = false;
    }

    private void flushCachedData(Jedis jedis) {
        try {
            jedis.getClient().getAll();
        } catch (RuntimeException ex) {
        }
    }

    @Override
    protected Client getClient(String key) {
        byte[] bKey = SafeEncoder.encode(key);

        return getClient(bKey);
    }

    @Override
    protected Client getClient(byte[] key) {
        Jedis jedis = getJedis(JedisClusterCRC16.getSlot(key));

        Client client = jedis.getClient();
        clients.add(client);

        return client;
    }

    private Jedis getJedis(int slot) {
        JedisPool pool = clusterInfoCache.getSlotPool(slot);

        // ����pool�ӻ����л�ȡJedis
        Jedis jedis = jedisMap.get(pool);
        if (null == jedis) {
            jedis = pool.getResource();
            jedisMap.put(pool, jedis);
        }

        hasDataInBuf = true;
        return jedis;
    }

    private static Field getField(Class<?> cls, String fieldName) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);

            return field;
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException("cannot find or access field '" + fieldName + "' from " + cls.getName(), e);
        }
    }

    @SuppressWarnings({"unchecked" })
    private static <T> T getValue(Object obj, Field field) {
        try {
            return (T)field.get(obj);
        } catch (IllegalArgumentException | IllegalAccessException e) {
        	logger.error("get value fail", e);
            throw new RuntimeException(e);
        }
    }   

    public static void main(String[] args) throws IOException {
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();
        nodes.add(new HostAndPort("127.0.0.1", 9379));
        nodes.add(new HostAndPort("127.0.0.1", 9380));

        JedisCluster jc = new JedisCluster(nodes);

        long s = System.currentTimeMillis();

        JedisClusterPipeline jcp = JedisClusterPipeline.pipelined(jc);
        jcp.refreshCluster();
        List<Object> batchResult = null;
        try {
            // batch write
            for (int i = 0; i < 10000; i++) {
                jcp.set("k" + i, "v1" + i);
            }
            jcp.sync();

            // batch read
            for (int i = 0; i < 10000; i++) {
                jcp.get("k" + i);
            }
            batchResult = jcp.syncAndReturnAll();
        } finally {
            jcp.close();
        }

        // output time 
        long t = System.currentTimeMillis() - s;
        System.out.println(t);

        System.out.println(batchResult.size());

        // ʵ��ҵ������У�closeҪ��finally�е�������֮����û��ôд������Ϊ��
        jc.close();
    }
}
