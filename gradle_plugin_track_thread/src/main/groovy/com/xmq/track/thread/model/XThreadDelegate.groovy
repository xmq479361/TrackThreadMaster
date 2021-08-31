package com.xmq.track.thread.model;

/**
 * @author xmqyeah
 * @CreateDate 2021/8/30 19:19
 */
class XThreadDelegate {
    String name
    String from
    String to
    String format
    /**
     * 代理的方法； 默认为<init>
     */
    String method
    XThreadDelegate(String name) {
        this.name = name
        method = "<init>"
    }

    void print(){
        println(">>> XThreadDelegate[$name]: $from, $to, $format")
    }
}
