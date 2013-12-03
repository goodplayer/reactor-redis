package com.github.nedis.callback;


import com.github.nedis.codec.Reply;

/**
 * Created with IntelliJ IDEA.
 * User: sunhao
 * Date: 13-12-3
 * Time: 下午5:30
 */
public interface Callback {
    public void onReceive(Reply reply);
}
