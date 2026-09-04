package com.OverZone.Scardaoni.entities;

import com.badlogic.gdx.math.Rectangle;


public class EnemyDummy {

    public static final float SIZE = 32f;
    public static final float MAX_HEALTH = 50f;
    public static final float FIRE_RANGE = 220f;
    public static final float FIRE_COOLDOWN = 1.6f;
    public static final float RESPAWN_DELAY = 5f;
    public static final int SCORE_VALUE = 100;

    private final float spawnX, spawnY;
    private float x, y;
    private float health;
    private boolean alive = true;
    private float fireCooldownRemaining = 0f;
    private float respawnRemaining = 0f;

    public EnemyDummy(float x, float y) {
        this.spawnX = x;
        this.spawnY = y;
        this.x = x;
        this.y = y;
        this.health = MAX_HEALTH;
    }

    public void update(float delta) {
        if (!alive) {
            respawnRemaining -= delta;
            if (respawnRemaining <= 0) {
                respawn();
            }
            return;
        }
        if (fireCooldownRemaining > 0) fireCooldownRemaining -= delta;
    }

    public boolean canFireAt(float targetX, float targetY) {
        if (!alive || fireCooldownRemaining > 0) return false;
        float dx = targetX - x;
        float dy = targetY - y;
        return (dx * dx + dy * dy) <= FIRE_RANGE * FIRE_RANGE;
    }

    public void resetFireCooldown() {
        fireCooldownRemaining = FIRE_COOLDOWN;
    }

    public void takeDamage(float amount) {
        if (!alive) return;
        health -= amount;
        if (health <= 0) {
            alive = false;
            respawnRemaining = RESPAWN_DELAY;
        }
    }

    private void respawn() {
        x = spawnX;
        y = spawnY;
        health = MAX_HEALTH;
        alive = true;
        fireCooldownRemaining = 0.5f;
    }

    public Rectangle getBounds() {
        return new Rectangle(x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public boolean isAlive() { return alive; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getHealth() { return health; }
    public float getHealthRatio() { return health / MAX_HEALTH; }
}
