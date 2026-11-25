package com.OverZone.Scardaoni.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;


public class GestorAudio {

    private Music musicaFondo;
    private Sound sonidoDisparo;
    private Sound sonidoGolpe;
    private Sound sonidoEliminacion;
    private Sound sonidoImpulso;
    private Sound sonidoEscudo;
    private Sound sonidoCuracion;
    private Sound sonidoCamuflaje;
    private Sound sonidoPotenciador;

    private float volumen = 0.6f;
    private boolean silenciado = false;

    public void cargar() {
        musicaFondo = Gdx.audio.newMusic(Gdx.files.internal("bg_music.wav"));
        musicaFondo.setLooping(true);

        sonidoDisparo = Gdx.audio.newSound(Gdx.files.internal("shoot.wav"));
        sonidoGolpe = Gdx.audio.newSound(Gdx.files.internal("hit.wav"));
        sonidoEliminacion = Gdx.audio.newSound(Gdx.files.internal("enemy_down.wav"));
        sonidoImpulso = Gdx.audio.newSound(Gdx.files.internal("dash.wav"));
        sonidoEscudo = Gdx.audio.newSound(Gdx.files.internal("shield.wav"));
        sonidoCuracion = Gdx.audio.newSound(Gdx.files.internal("heal.wav"));
        sonidoCamuflaje = Gdx.audio.newSound(Gdx.files.internal("camo.wav"));
        sonidoPotenciador = Gdx.audio.newSound(Gdx.files.internal("powerup.wav"));

        aplicarVolumenMusica();
    }

    public void reproducirMusica() {
        if (!musicaFondo.isPlaying()) {
            musicaFondo.play();
        }
    }

    public void pausarMusica() {
        musicaFondo.pause();
    }

    public void sonarDisparo() { reproducir(sonidoDisparo); }
    public void sonarGolpe() { reproducir(sonidoGolpe); }
    public void sonarEliminacion() { reproducir(sonidoEliminacion); }
    public void sonarImpulso() { reproducir(sonidoImpulso); }
    public void sonarEscudo() { reproducir(sonidoEscudo); }
    public void sonarCuracion() { reproducir(sonidoCuracion); }
    public void sonarCamuflaje() { reproducir(sonidoCamuflaje); }
    public void sonarPotenciador() { reproducir(sonidoPotenciador); }

    private void reproducir(Sound sonido) {
        if (!silenciado) {
            sonido.play(volumen);
        }
    }

    public void setVolumen(float nuevoVolumen) {
        volumen = Math.max(0f, Math.min(1f, nuevoVolumen));
        aplicarVolumenMusica();
    }


    public void setVolumenXBarra(float nuevoVolumen) {
        setVolumen(nuevoVolumen);
        if (silenciado && volumen > 0f) {
            silenciado = false;
            aplicarVolumenMusica();
        }
    }

    public void mutearDesmutear() {
        silenciado = !silenciado;
        aplicarVolumenMusica();
    }

    public void subirVolumen() {
        setVolumen(volumen + 0.1f);
    }

    public void bajarVolumen() {
        setVolumen(volumen - 0.1f);
    }

    private void aplicarVolumenMusica() {
        musicaFondo.setVolume(silenciado ? 0f : volumen);
    }

    public boolean estaSilenciado() { return silenciado; }
    public float getVolumen() { return volumen; }

    public void dispose() {
        musicaFondo.dispose();
        sonidoDisparo.dispose();
        sonidoGolpe.dispose();
        sonidoEliminacion.dispose();
        sonidoImpulso.dispose();
        sonidoEscudo.dispose();
        sonidoCuracion.dispose();
        sonidoCamuflaje.dispose();
        sonidoPotenciador.dispose();
    }
}
