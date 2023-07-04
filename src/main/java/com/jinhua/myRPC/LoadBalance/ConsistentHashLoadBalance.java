package com.jinhua.myRPC.LoadBalance;

import com.jinhua.myRPC.common.RPCRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希算法，就是先将服务节点映射到[0,2^32-1]的哈希环上，再将新请求的key值映射到哈希环上，该请求就会选择比这个key值大的最近的一个服务节点
 */
public class ConsistentHashLoadBalance implements LoadBalance{
    // 使用 ConcurrentHashMap来存储不同的服务对应ConsistentHashSelector实例
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    public String doSelect(List<String> addressList, RPCRequest request) {
        // 获取 serviceAddresses 的哈希码
        int identityHashCode = System.identityHashCode(addressList);
        String interfaceName = request.getInterfaceName();

        // 根据服务的名字找对应的 ConsistentHashSelector 实例
        ConsistentHashSelector selector = selectors.get(interfaceName);
        // 检查是否需要更新 ConsistentHashSelector 实例
        if (selector == null || selector.identityHashCode != identityHashCode) {
            // 如果 selector 不存在或者哈希码不匹配，创建一个新的 ConsistentHashSelector 实例并存入 selectors 中
            selectors.put(interfaceName, new ConsistentHashSelector(addressList, 160, identityHashCode));
            selector = selectors.get(interfaceName);
        }
        // 调用 selector 的 select 方法进行选择，并返回结果
        return selector.select(interfaceName + Arrays.stream(request.getParams()));
    }

    static class ConsistentHashSelector {
        // 使用TreeMap来存储虚拟节点和真实节点之间的映射关系
        private final TreeMap<Long, String> virtualInvokers;

        // 用于标识ConsistentHashSelector实例的哈希码
        private final int identityHashCode;

        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            // 遍历 invokers 列表，为每个 invoker 创建 replicaNumber/4 个虚拟节点，并将虚拟节点和真实节点的映射关系存入 virtualInvokers中
            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        // 哈希值 为key , 服务地址为 value 放在TreeMap数据结构中
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        // 计算字符串的 MD5 哈希值
        static byte[] md5(String key) {
            MessageDigest md;
            try {
                // 获取 MD5消息摘要 算法实例
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                // 使用MD5算法对字节数组进行哈希计算
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            return md.digest();
        }

        // 计算哈希值
        private long hash(byte[] digest, int number) {
            // 这个就是将字符串的MD5哈希值的字节数组，按4个字节转化为一个32b的整数最后合并起来形成一个long的哈希值
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        // 根据 rpcServiceKey 选择一个服务地址
        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest, 0));
        }

        // 根据哈希值选择一个服务地址
        public String selectForKey(long hashCode) {
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();

            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }

    }
}
