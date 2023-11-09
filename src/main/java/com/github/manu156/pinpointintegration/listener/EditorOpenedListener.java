package com.github.manu156.pinpointintegration.listener;

import com.github.manu156.pinpointintegration.editor.RenderQueue;
import com.github.manu156.pinpointintegration.editor.util.RenderUtil;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.impl.event.MarkupModelListener;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;


import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.List;

public class EditorOpenedListener implements FileEditorManagerListener {

    @Override
    public void fileOpenedSync(@NotNull FileEditorManager source, @NotNull VirtualFile file,
                               @NotNull Pair<FileEditor[], FileEditorProvider[]> editors) {
        FileEditor[] fileEditors = editors.first;

        RenderQueue rq = ApplicationManager.getApplication().getService(RenderQueue.class);
        List<JsonObject> js = rq.poll();
        if (null == js || js.isEmpty())
            return;

        RenderUtil.renderHelper(js, fileEditors, source.getProject());
    }
}