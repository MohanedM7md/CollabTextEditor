package com.editor.collabtexteditor.controllers;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import lombok.Getter;

public class UserIndicator {
    private final String userId;
    private final int position;
    private final Color color;
    @Getter
    private final Rectangle indicator;

    public UserIndicator(String userId, int position, Color color) {
        this.userId = userId;
        this.position = position;
        this.color = color;
        this.indicator = new Rectangle(2, 20, color);
        this.indicator.setOpacity(0.5);
    }

    public void updatePosition(int newPosition) {
        // Update logic for the indicator position
    }
}