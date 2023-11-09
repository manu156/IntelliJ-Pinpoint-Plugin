package com.github.manu156.pinpointintegration.editor.data;

import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class MyColorConverter extends Converter<Color> {
    @Override
    public @Nullable Color fromString(@NotNull String value) {
        return new Color(Integer.parseInt(value));
    }

    @Override
    public @Nullable String toString(@NotNull Color value) {
        return String.valueOf(value.getRGB());
    }
}
