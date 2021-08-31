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

    private TrackThreadCore() {
        innerClassTo.clear()
        replaces.clear()
    }
    XThreadExtension extension
    Map<String, String> innerClassTo = new HashMap<>()
    Map<String, String> replaces = new HashMap<>()

    void setExtension(XThreadExtension extension) {
        extension.generates.each {
            replaces.put(it.from, it.to)
        }
        this.extension = extension
        System.err.println(replaces)
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
        return extension.generates.find { it.from == type }
    }

    static boolean isExclude(String path) {
        return getInstance().extension != null && getInstance().extension.excludes.find {
            System.out.println("isExclude: ${path.replace("/", ".")}, ${it.replace("/", ".")}")
            return path.replace("/", ".") == it.replace("/", ".")
        } != null
    }
}
