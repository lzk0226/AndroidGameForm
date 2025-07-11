package com.app.gameform.network;

/**
 * API回调接口
 */
public interface ApiCallback<T> {
    void onSuccess(T data);
    void onError(String error);
}