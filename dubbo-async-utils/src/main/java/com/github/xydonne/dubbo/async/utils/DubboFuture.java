package com.github.xydonne.dubbo.async.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author DonneyYoung
 */
public class DubboFuture<T> implements Future<T> {

    private Future<T> future;

    private DubboCallback<T> callback;

    public DubboFuture(Future<T> future) {
        this.future = future;
    }

    public DubboFuture(Future<T> future, DubboCallback<T> callback) {
        this.future = future;
        this.callback = callback;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    public T done() {
        if (null != callback)
            callback.waitUntilDone();
        try {
            return get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public T done(long timeout, TimeUnit unit) {
        if (null != callback)
            callback.waitUntilDone(unit.toMillis(timeout));
        try {
            return get(timeout, unit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
