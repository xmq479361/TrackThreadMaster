package com.xmq.track.thread

import com.android.build.gradle.AppExtension
import com.xmq.track.thread.model.XThreadExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class TrackThreadPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.logger.lifecycle("${project.name} apply TrackThreadPlugin")
        if (project == project.getRootProject()) {
            return
        }
        def threadExtension = project.extensions.create("threadTrack", XThreadExtension, project)
        project.logger.lifecycle("${project.name} apply threadExtension.enabled ${threadExtension.enabled}")
        TrackThreadCore.getInstance().setExtension(threadExtension)
//        if (threadExtension.enabled) {
            def android = project.extensions.getByType(AppExtension)
            android.registerTransform(new XTrackThreadTransform())
//        }
    }
}