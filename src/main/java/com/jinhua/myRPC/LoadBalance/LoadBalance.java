package com.jinhua.myRPC.LoadBalance;

import com.jinhua.myRPC.common.RPCRequest;

import java.util.List;

/**
 * 负载均衡器
 */
public interface LoadBalance {
    // 在节点列表里面选择一个客户端节点地址
    String doSelect(List<String> addressList, RPCRequest request);
}
