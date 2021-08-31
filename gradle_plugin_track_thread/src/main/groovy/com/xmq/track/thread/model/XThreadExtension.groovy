package com.xmq.track.thread.model

import com.xmq.track.thread.TrackThreadCore
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * @author xmqyeah
 * @CreateDate 2021/8/30 19:17
 */
class XThreadExtension {
    boolean enabled
    String format
    /**
     * 过滤的类package/name前缀
     */
    List<String> excludes = new LinkedList<>()

    NamedDomainObjectContainer<XThreadDelegate> generates

    XThreadExtension(Project project) {
        generates = project.container(XThreadDelegate)
//        println(">>>XThreadExtension generates: $enabled")
//        generates.each {it ->
//            it.print()
//        }
    }

    /**
     * 过滤的类package/name前缀
     */
    void excludes(String... clzs) {
        this.excludes.addAll(clzs)
    }

    void generates(Action<NamedDomainObjectContainer<XThreadDelegate>> action) {
        action.execute(generates)
        println(">>>XThreadExtension generates(): $enabled")
        generates.each {it ->
            it.print()
        }
        TrackThreadCore.instance.setExtension(this)
    }

}
