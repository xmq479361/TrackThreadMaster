package com.xmq.thread;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author xmqyeah
 * @CreateDate 2021/8/31 23:50
 */
public class DelegateTimer extends Timer {
    public String invokeMethodName;

    public DelegateTimer() {
        this.invokeMethodName = invokeMethodName;
    }
    public DelegateTimer(String invokeMethodName) {
        this.invokeMethodName = invokeMethodName;
    }

    public DelegateTimer(boolean isDaemon, String invokeMethodName) {
        super(isDaemon);
        this.invokeMethodName = invokeMethodName;
    }

    public DelegateTimer(String name, String invokeMethodName) {
        super(name);
        this.invokeMethodName = invokeMethodName;
    }

    public DelegateTimer(String name, boolean isDaemon, String invokeMethodName) {
        super(name, isDaemon);
        this.invokeMethodName = invokeMethodName;
    }

    public void schedule(TimerTask task, long delay, String invokeMethodName) {
        this.invokeMethodName = invokeMethodName;
        super.schedule(task, delay);
    }
}
