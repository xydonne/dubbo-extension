package com.github.xydonne.dubbo.async.utils;

import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;

import java.util.concurrent.Future;

/**
 * @author DonneyYoung
 */
public class DubboAsyncUtils {

    /**
     * 异步请求
     *
     * @param response dubbo返回对象
     * @param <E>         真正需要的对象
     * @return 真正需要的对象
     */
    @SuppressWarnings("unchecked")
    public static <E> DubboFuture<E> async(E response) {
        return new DubboFuture<>((Future<E>) RpcContext.getContext().getFuture());
    }

    /**
     * 异步请求
     *
     * @param response dubbo返回对象
     * @param <E>         真正需要的对象
     * @return 真正需要的对象
     */
    @SuppressWarnings("unchecked")
    public static <E> DubboFuture<E> async(E response, DubboCallback<E> responseCallback) {
        ((FutureAdapter) RpcContext.getContext().getFuture()).getFuture().setCallback(responseCallback);
        return new DubboFuture<>((Future<E>) RpcContext.getContext().getFuture(), responseCallback);
    }
}
