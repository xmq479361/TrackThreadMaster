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
    NamedDomainObjectContainer<XThreadDelegate> generates

    XThreadExtension(Project project) {
        generates = project.container(XThreadDelegate)
//        println(">>>XThreadExtension generates: $enabled")
//        generates.each {it ->
//            it.print()
//        }
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
