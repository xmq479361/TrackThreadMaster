package com.xmq.track.thread.model

/**
 * @author xmqyeah
 * @CreateDate 2021/8/30 19:37
 */
class DelegateConfig {
    String from
    String to
    String format


    @Override
    public String toString() {
        return "Delegator{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}
