package com.github.manu156.pinpointintegration.editor;

import com.google.gson.JsonObject;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;

import java.util.*;

public class RenderQueue {
    private final Map<String, JsonObject> queue = new HashMap<>();
    private final List<Disposable> disposableList = new ArrayList<>();
    private final Set<String> addedInQueueHash = new HashSet<>();
    private final Set<String> rendered = new HashSet<>();

    public void add(JsonObject jsonObject, String agentId, String spanId, String traceId, String fTS) {
        if (!addedInQueueHash.contains(agentId+spanId+traceId+fTS)) {
            addedInQueueHash.add(agentId+spanId+traceId+fTS);
            queue.put(agentId+spanId+traceId+fTS, jsonObject);
        }
    }

    public void clear() {
        queue.clear();
        addedInQueueHash.clear();
        rendered.clear();
    }

    public List<JsonObject> poll() {
        return new ArrayList<>(queue.values());
    }

    public List<JsonObject> popNotRendered() {
        List<JsonObject> resp = queue.entrySet().stream()
                .filter(t -> !rendered.contains(t.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        rendered.addAll(queue.keySet());
        return resp;
    }

    public void disposeAll() {
        for (Disposable disposable : disposableList) {
            Disposer.dispose(disposable);
        }
        disposableList.clear();
    }

    public void addToDisposable(Disposable disposable) {
        if (null != disposable)
            disposableList.add(disposable);
    }
}
