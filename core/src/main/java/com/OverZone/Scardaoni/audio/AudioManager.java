package com.OverZone.Scardaoni.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;


public class AudioManager {

    private Music backgroundMusic;
    private Sound shootSound;
    private Sound hitSound;
    private Sound enemyDownSound;
    private Sound dashSound;
    private Sound shieldSound;
    private Sound healSound;
    private Sound camoSound;
    private Sound powerUpSound;

    private float volume = 0.6f;
    private boolean muted = false;

    public void load() {
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("bg_music.wav"));
        backgroundMusic.setLooping(true);

        shootSound = Gdx.audio.newSound(Gdx.files.internal("shoot.wav"));
        hitSound = Gdx.audio.newSound(Gdx.files.internal("hit.wav"));
        enemyDownSound = Gdx.audio.newSound(Gdx.files.internal("enemy_down.wav"));
        dashSound = Gdx.audio.newSound(Gdx.files.internal("dash.wav"));
        shieldSound = Gdx.audio.newSound(Gdx.files.internal("shield.wav"));
        healSound = Gdx.audio.newSound(Gdx.files.internal("heal.wav"));
        camoSound = Gdx.audio.newSound(Gdx.files.internal("camo.wav"));
        powerUpSound = Gdx.audio.newSound(Gdx.files.internal("powerup.wav"));

        applyMusicVolume();
    }

    public void playMusic() {
        if (!backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    public void pauseMusic() {
        backgroundMusic.pause();
    }

    public void playShoot() { play(shootSound); }
    public void playHit() { play(hitSound); }
    public void playEnemyDown() { play(enemyDownSound); }
    public void playDash() { play(dashSound); }
    public void playShield() { play(shieldSound); }
    public void playHeal() { play(healSound); }
    public void playCamo() { play(camoSound); }
    public void playPowerUp() { play(powerUpSound); }

    private void play(Sound sound) {
        if (!muted) {
            sound.play(volume);
        }
    }

    public void setVolume(float newVolume) {
        volume = Math.max(0f, Math.min(1f, newVolume));
        applyMusicVolume();
    }


    public void setVolumeXBarra(float newVolume) {
        setVolume(newVolume);
        if (muted && volume > 0f) {
            muted = false;
            applyMusicVolume();
        }
    }

    public void MutearDesmutear() {
        muted = !muted;
        applyMusicVolume();
    }

    public void SubirVolumen() {
        setVolume(volume + 0.1f);
    }

    public void BajarVolumen() {
        setVolume(volume - 0.1f);
    }

    private void applyMusicVolume() {
        backgroundMusic.setVolume(muted ? 0f : volume);
    }

    public boolean isMuted() { return muted; }
    public float getVolume() { return volume; }

    public void dispose() {
        backgroundMusic.dispose();
        shootSound.dispose();
        hitSound.dispose();
        enemyDownSound.dispose();
        dashSound.dispose();
        shieldSound.dispose();
        healSound.dispose();
        camoSound.dispose();
        powerUpSound.dispose();
    }
}
