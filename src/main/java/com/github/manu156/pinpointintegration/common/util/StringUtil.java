package com.github.manu156.pinpointintegration.common.util;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StringUtil {

    private StringUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final Logger logger = Logger.getInstance(StringUtil.class);

    @Nullable
    public static String getString(HttpEntity entity) {
        if (entity == null)
            return null;

        try (InputStream inStream = entity.getContent()) {
            return IOUtils.toString(inStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.info("Failed while reading stream.", e);
        }
        return null;
    }
}
