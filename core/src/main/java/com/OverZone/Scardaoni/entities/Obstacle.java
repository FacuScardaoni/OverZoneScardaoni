package com.OverZone.Scardaoni.entities;

import com.badlogic.gdx.math.Rectangle;


public class Obstacle {

    private final Rectangle bounds;

    public Obstacle(float x, float y, float width, float height) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    public Rectangle getBounds() {
        return bounds;
    }
}
