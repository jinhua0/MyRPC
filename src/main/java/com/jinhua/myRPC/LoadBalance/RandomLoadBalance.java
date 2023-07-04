package com.jinhua.myRPC.LoadBalance;

import com.jinhua.myRPC.common.RPCRequest;

import java.util.List;
import java.util.Random;

/**
 * 随机的负载均衡策略
 */
public class RandomLoadBalance implements LoadBalance{
    @Override
    public String doSelect(List<String> addressList, RPCRequest request) {
        Random random = new Random();
        int choose = 0;
        if (addressList.size() > 0) {
            choose = random.nextInt(addressList.size());
        }
        System.out.println("负载均衡选择了" + choose + "服务器," + addressList.get(choose));
        return addressList.get(choose);
    }
}
