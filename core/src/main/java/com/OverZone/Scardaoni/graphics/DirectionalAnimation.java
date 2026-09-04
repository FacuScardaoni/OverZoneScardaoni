package com.OverZone.Scardaoni.graphics;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;


public class DirectionalAnimation {

    public static final int DOWN = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    public static final int UP = 3;

    private final Texture sheet;
    private final Animation<TextureRegion>[] walkByDirection;
    private final TextureRegion[] idleByDirection;
    private float stateTime = 0f;

    @SuppressWarnings("unchecked")
    public DirectionalAnimation(Texture sheet, int frameWidth, int frameHeight, int frameCols, float frameDuration) {
        this.sheet = sheet;
        TextureRegion[][] grid = TextureRegion.split(sheet, frameWidth, frameHeight);

        walkByDirection = new Animation[4];
        idleByDirection = new TextureRegion[4];
        for (int dir = 0; dir < 4 && dir < grid.length; dir++) {
            TextureRegion[] frames = new TextureRegion[frameCols];
            System.arraycopy(grid[dir], 0, frames, 0, Math.min(frameCols, grid[dir].length));
            walkByDirection[dir] = new Animation<>(frameDuration, frames);
            walkByDirection[dir].setPlayMode(Animation.PlayMode.LOOP);
            idleByDirection[dir] = frames[0];
        }
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public TextureRegion getFrame(int direction, boolean moving) {
        direction = Math.max(0, Math.min(3, direction));
        if (!moving) {
            return idleByDirection[direction];
        }
        return walkByDirection[direction].getKeyFrame(stateTime);
    }

    public void dispose() {
        sheet.dispose();
    }
}
