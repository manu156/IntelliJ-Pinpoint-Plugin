package com.github.manu156.pinpointintegration.window.plot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class GraphData {
    List<Long> xData = new ArrayList<>();
    List<Long> yData = new ArrayList<>();
    List<Boolean> success = new ArrayList<>();
    List<Long> count = new ArrayList<>();
    List<String> tid = new ArrayList<>();


    /**
     *
     * Reference: <a href="https://github.com/pinpoint-apm/pinpoint/issues/3824">github.com/../issues/3824</a>
     * @param s
     * @param toTime
     * @param offset
     */
    public GraphData(List<JsonObject> s, long toTime, long offset) {
        for (JsonObject jsonObject: s) {
            JsonObject scatterJson = jsonObject.get("scatter").getAsJsonObject();
            JsonArray dotList = scatterJson.get("dotList").getAsJsonArray();
            JsonObject metaDataJson = scatterJson.get("metadata").getAsJsonObject();
            for (int i = 0; i < dotList.size(); i++) {
                JsonArray dt = dotList.get(i).getAsJsonArray();
                xData.add(dt.get(0).getAsLong() + toTime - offset);
                yData.add(dt.get(1).getAsLong());
                success.add(dt.get(4).getAsInt() == 1);
                count.add(dt.get(5).getAsLong());
                Long metaDataId = dt.get(2).getAsLong();
                tid.add(metaDataJson.get(metaDataId.toString()).getAsJsonArray().get(1).getAsString() + "^" +
                        metaDataJson.get(metaDataId.toString()).getAsJsonArray().get(2).getAsString() + "^" +
                        dt.get(3).getAsString());
            }
        }
    }
}
