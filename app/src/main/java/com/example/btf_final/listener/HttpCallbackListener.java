package com.example.btf_final.listener;

public interface HttpCallbackListener {

    void onFinish(String response);

    void onError(Exception e);

}