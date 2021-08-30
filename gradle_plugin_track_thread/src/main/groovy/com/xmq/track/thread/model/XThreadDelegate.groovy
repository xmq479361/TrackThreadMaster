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
    XThreadDelegate(String name) {
        this.name = name
//        format = "${methodName}"
    }

    void print(){
        println(">>> XThreadDelegate[$name]: $from, $to, $format")
    }
}
