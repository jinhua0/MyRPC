package com.jinhua.myRPC.client;

import com.jinhua.myRPC.common.RPCRequest;
import com.jinhua.myRPC.common.RPCResponse;

public interface RPCClient {
    // 客户端接口，功能发送RPCRequest请求，访问服务器得到RPCResponse
    RPCResponse sendRequest(RPCRequest request);
}
