package com.jinhua.myRPC.LoadBalance;

import com.jinhua.myRPC.common.RPCRequest;

import java.util.List;

/**
 * 轮询的负载均衡策略
 */
public class RoundLoadBalance implements LoadBalance{
    private int choose = -1;
    @Override
    public String doSelect(List<String> addressList, RPCRequest request) {
        choose++;
        choose = choose % addressList.size();
        return addressList.get(choose);
    }
}
