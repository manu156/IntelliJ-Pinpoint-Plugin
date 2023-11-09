package com.github.manu156.pinpointintegration.common.util;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.NotSupportedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Optional;

public class RestUtil {

    private RestUtil() {
        throw new NotSupportedException("Utility Class!");
    }

    private static final Logger logger = Logger.getInstance(RestUtil.class);

    public static <T> Optional<T> httpPost(String uri, HttpEntity httpEntity, Class<T> clazz) {
        HttpPost httppost = new HttpPost(uri);
        httppost.setEntity(httpEntity);
        return executeHttpReq(uri, httppost, clazz);
    }

    public static <T> Optional<T> httpGet(String uri, Class<T> clazz) {
        HttpGet httpGet = new HttpGet(uri);
        return executeHttpReq(uri, httpGet, clazz);
    }

    private static <T> Optional<T> executeHttpReq(String uri, HttpUriRequest httpUriRequest, Class<T> clazz) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpResponse response = httpclient.execute(httpUriRequest);
            HttpEntity entity = response.getEntity();

            String responseString = StringUtil.getString(entity);
            if (responseString != null) {
                return Optional.of(new Gson().fromJson(responseString, clazz));
            }
        } catch (IOException e) {
            logger.info("Failed to execute HTTP request", e);
        }
        return Optional.empty();
    }
}
