package com.OverZone.Scardaoni.entities;

import com.badlogic.gdx.math.Rectangle;
import com.OverZone.Scardaoni.graphics.DirectionalAnimation;


public class Player {

    public static final float SIZE = 24f;
    public static final float SPEED = 140f; // unidades/seg
    public static final float MAX_HEALTH = 100f;

    public static final float DASH_SPEED_MULTIPLIER = 3.2f;
    public static final float DASH_DURATION = 0.18f;
    public static final float DASH_COOLDOWN = 3.0f;

    private float x, y;
    private float health = MAX_HEALTH;
    private int facingDirection = DirectionalAnimation.DOWN;
    private boolean moving = false;

    private float dashTimeRemaining = 0f;
    private float dashCooldownRemaining = 0f;
    private float dashDirX, dashDirY;

    private float invulnerableTimeRemaining = 0f;

    public Player(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Rectangle getBounds() {
        return new Rectangle(x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public void update(float delta) {
        if (dashTimeRemaining > 0) dashTimeRemaining -= delta;
        if (dashCooldownRemaining > 0) dashCooldownRemaining -= delta;
        if (invulnerableTimeRemaining > 0) invulnerableTimeRemaining -= delta;
    }

    public void tryStartDash(float dirX, float dirY) {
        if (dashCooldownRemaining <= 0f && (dirX != 0 || dirY != 0)) {
            dashTimeRemaining = DASH_DURATION;
            dashCooldownRemaining = DASH_COOLDOWN;
            float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            dashDirX = dirX / len;
            dashDirY = dirY / len;
        }
    }

    public boolean isDashing() {
        return dashTimeRemaining > 0f;
    }

    public float getCurrentSpeed() {
        return isDashing() ? SPEED * DASH_SPEED_MULTIPLIER : SPEED;
    }

    /** Dirección normalizada del impulso; solo válida mientras isDashing(). */
    public float getDashDirX() { return dashDirX; }
    public float getDashDirY() { return dashDirY; }

    public void move(float dx, float dy) {
        x += dx;
        y += dy;
    }

    public void takeDamage(float amount) {
        if (invulnerableTimeRemaining > 0) return;
        health -= amount;
        if (health < 0) health = 0;
        invulnerableTimeRemaining = 0.4f;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public boolean isInvulnerable() {
        return invulnerableTimeRemaining > 0f;
    }

    public float getDashCooldownProgress() {
        // 0 = listo para usar, 1 = recién usado
        return Math.max(0f, dashCooldownRemaining / DASH_COOLDOWN);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public float getHealth() { return health; }
    public int getFacingDirection() { return facingDirection; }
    public void setFacingDirection(int facingDirection) { this.facingDirection = facingDirection; }
    public boolean isMoving() { return moving; }
    public void setMoving(boolean moving) { this.moving = moving; }
}
