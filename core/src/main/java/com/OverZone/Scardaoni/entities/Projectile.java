package com.OverZone.Scardaoni.entities;

import com.badlogic.gdx.math.Rectangle;


public class Projectile {

    public static final float SIZE = 8f;
    public static final float SPEED = 320f;
    public static final float DAMAGE_PLAYER_TO_ENEMY = 25f;
    public static final float DAMAGE_ENEMY_TO_PLAYER = 10f;

    private float x, y;
    private final float dirX, dirY;
    private final boolean fromPlayer;
    private boolean alive = true;

    public Projectile(float x, float y, float dirX, float dirY, boolean fromPlayer) {
        this.x = x;
        this.y = y;
        this.fromPlayer = fromPlayer;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len == 0) len = 1;
        this.dirX = dirX / len;
        this.dirY = dirY / len;
    }

    public void update(float delta) {
        x += dirX * SPEED * delta;
        y += dirY * SPEED * delta;
    }

    public Rectangle getBounds() {
        return new Rectangle(x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public boolean isFromPlayer() { return fromPlayer; }
    public boolean isAlive() { return alive; }
    public void kill() { alive = false; }
    public float getX() { return x; }
    public float getY() { return y; }
}
