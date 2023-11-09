package com.github.manu156.pinpointintegration.editor.render;

import com.github.manu156.pinpointintegration.editor.data.EffectType;
import com.github.manu156.pinpointintegration.editor.data.MyColorConverter;

import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class RenderData {

    public final int numberOfWhitespaces;
    public final @NotNull EffectType effectType;
    public final @NotNull String description;
    public final @Nullable Icon icon;

    public boolean showGutterIcon = true;

    public boolean showText = true;
    public boolean showBackground = true;
    public boolean showEffect = true;
    @Attribute(converter = MyColorConverter.class)
    public @NotNull Color textColor = Color.RED;
    @Attribute(converter = MyColorConverter.class)
    public @NotNull Color backgroundColor = Color.GREEN;
    @Attribute(converter = MyColorConverter.class)
    public @NotNull Color effectColor = Color.ORANGE;

    public RenderData(boolean showGutterIcon, boolean showText, boolean showBackground, boolean showEffect,
                      @NotNull Color textColor, @NotNull Color backgroundColor, @NotNull Color effectColor, int numberOfWhitespaces,
                      @NotNull EffectType effectType, @NotNull String description, @Nullable Icon icon) {
        this.showGutterIcon = showGutterIcon;
        this.showText = showText;
        this.showBackground = showBackground;
        this.showEffect = showEffect;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
        this.effectColor = effectColor;
        this.numberOfWhitespaces = numberOfWhitespaces;
        this.effectType = effectType;
        this.description = description;
        this.icon = icon;
    }
}
