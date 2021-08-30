package com.xmq.track.thread

import com.xmq.track.thread.model.XThreadDelegate
import com.xmq.track.thread.model.XThreadExtension

/**
 * @author xmqyeah* @CreateDate 2021/8/30 20:03
 */
class TrackThreadCore {
    static TrackThreadCore INSTANCE

    static TrackThreadCore getInstance() {
        if (INSTANCE == null) {
            synchronized (TrackThreadCore.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TrackThreadCore()
                }
            }
        }
        return INSTANCE
    }

    XThreadExtension extension
    Map<String, String> replaces = new HashMap<>()
     void setExtension(XThreadExtension extension) {
        extension.generates.each {
            replaces.put(it.from, it.to)
        }
         println(replaces)
         println(replaces.keySet())
    }

    void put(String key, String value) {
        replaces.put(key, value)
    }
    boolean containsKey(String type) {
        return replaces.containsKey(type)
    }

    String findTo(String type) {
        return replaces.get(type)
    }

    XThreadDelegate find(String type) {
        return extension.generates.find {it.from == type}
    }
}
