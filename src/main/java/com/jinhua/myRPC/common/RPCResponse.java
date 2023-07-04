package com.jinhua.myRPC.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务端响应数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RPCResponse implements Serializable {
    // 响应状态码
    private int code;

    // 响应信息
    private String message;

    // data的数据类型
    private Class<?> dataType;

    // 返回的数据抽象为Object类型
    private Object data;

    public static RPCResponse success(Object data) {
        return RPCResponse.builder().code(200).data(data).dataType(data.getClass()).build();
    }

    public static RPCResponse fail() {
        return RPCResponse.builder().code(500).message("服务器发生错误").build();
    }


}
