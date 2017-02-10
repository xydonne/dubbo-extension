package com.github.xydonne.dubbo.async.utils;

import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.rpc.Result;

/**
 * @author DonneyYoung
 */
public abstract class DubboCallback<T> implements ResponseCallback {

    protected volatile boolean done = false;

    public abstract void apply(T response);

    @Override
    @SuppressWarnings("unchecked")
    public void done(Object result) {
        try {
            this.apply((T) ((Result) result).getValue());
        } finally {
            synchronized (this) {
                done = true;
                this.notifyAll();
            }
        }
    }

    @Override
    public void caught(Throwable throwable) {
        synchronized (this) {
            done = true;
            this.notifyAll();
        }
    }

    public void waitUntilDone() {
        waitUntilDone(0);
    }

    public void waitUntilDone(long timeout) {
        synchronized (this) {
            while (!done) {
                try {
                    this.wait(timeout);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
