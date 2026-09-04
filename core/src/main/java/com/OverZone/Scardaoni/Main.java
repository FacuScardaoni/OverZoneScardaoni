package com.OverZone.Scardaoni;

import com.badlogic.gdx.Game;
import com.OverZone.Scardaoni.audio.AudioManager;
import com.OverZone.Scardaoni.screens.MenuScreen;

public class Main extends Game {

    private AudioManager audioManager;

    @Override
    public void create() {
        audioManager = new AudioManager();
        audioManager.load();
        setScreen(new MenuScreen(this));
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    @Override
    public void dispose() {
        super.dispose();
        audioManager.dispose();
    }
}
