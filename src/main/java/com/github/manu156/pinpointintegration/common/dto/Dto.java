package com.github.manu156.pinpointintegration.common.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

public class Dto {
    public JsonArray callStackJson;
    public Map<String, Map<String, Integer>> classNamesToMethodSigToIndex;
    public JsonObject callStackIndexJson;
    public Map<Integer, List<Integer>> parentToChildIndex;
    public String apiEndPoint;
    public Long apiTotalTime;

    public Dto(JsonArray callStackJson, Map<String, Map<String, Integer>> classNamesToMethodSigToIndex,
               JsonObject callStackIndexJson, Map<Integer, List<Integer>> parentToChildIndex, String apiEndPoint,
               Long apiTotalTime) {
        this.classNamesToMethodSigToIndex = classNamesToMethodSigToIndex;
        this.callStackJson = callStackJson;
        this.callStackIndexJson = callStackIndexJson;
        this.parentToChildIndex = parentToChildIndex;
        this.apiEndPoint = apiEndPoint;
        this.apiTotalTime = apiTotalTime;
    }
}
