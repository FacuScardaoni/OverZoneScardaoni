package com.OverZone.Scardaoni.pantallas;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.JuegoOverZone;
import com.OverZone.Scardaoni.interfaz.KitUi;
import com.OverZone.Scardaoni.compartido.TipoPersonaje;
import com.OverZone.Scardaoni.compartido.ModoJuego;
import com.OverZone.Scardaoni.compartido.FasePartida;
import com.OverZone.Scardaoni.compartido.Equipo;
import com.OverZone.Scardaoni.red.RedCliente;
import com.OverZone.Scardaoni.compartido.EstadoMundo;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultados de la partida. Los datos vienen del servidor. Pasados unos
 * segundos, el servidor vuelve a la sala de espera y esta pantalla la sigue,
 * así se puede jugar otra partida sin cerrar el programa.
 */
public class PantallaResultados extends PantallaMenuBase {

    private final Rectangle botonSalir = new Rectangle(170, 20, 140, 28);

    public PantallaResultados(JuegoOverZone juego) {
        super(juego, 2f);
    }

    @Override
    public void show() {
        super.show();
        juego.getGestorAudio().pausarMusica();
        if (juego.getRed() != null) juego.getRed().setListo(false);
    }

    @Override
    protected InputProcessor crearProcesadorEntrada() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int codigoTecla) {
                if (codigoTecla == Input.Keys.ESCAPE) {
                    salir();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
                if (boton == Input.Buttons.LEFT && clickeado(botonSalir, pantallaX, pantallaY)) {
                    salir();
                    return true;
                }
                return false;
            }
        };
    }

    private void salir() {
        juego.desconectar();
        juego.reemplazarPantalla(new PantallaMenu(juego));
    }

    @Override
    protected boolean actualizar(float delta) {
        return juego.seguirServidor(FasePartida.RESULTADOS);
    }

    @Override
    protected void dibujarFormas(Vector3 mouse) {
        dibujarPanel(60, 64, 360, 92, KitUi.CIAN);
        dibujarBoton(botonSalir, mouse, 0.30f, 0.30f, 0.35f);
    }

    @Override
    protected void dibujarTexto(Vector3 mouse) {
        RedCliente conexion = juego.getRed();
        EstadoMundo estadoActual = conexion == null ? null : conexion.getUltimoEstado();
        if (estadoActual == null) return;

        EstadoMundo.EstadoJugador yo = estadoActual.buscarJugador(conexion.getJugadorId());
        boolean todosContraTodos = estadoActual.getModo() == ModoJuego.TODOS_CONTRA_TODOS;

        // En Equipos, "winner" es el equipo ganador; en Todos contra todos, el id del jugador.
        String titulo;
        Color color;
        if (estadoActual.ganador == Equipo.NINGUNO) {
            titulo = "EMPATE";
            color = Color.LIGHT_GRAY;
        } else if (todosContraTodos) {
            EstadoMundo.EstadoJugador w = estadoActual.buscarJugador(estadoActual.ganador);
            if (yo != null && estadoActual.ganador == yo.id) {
                titulo = "¡GANASTE!";
                color = Color.GOLD;
            } else {
                titulo = "GANÓ " + (w == null ? "?" : w.nombre.toUpperCase());
                color = Color.SCARLET;
            }
        } else if (yo != null && estadoActual.ganador == yo.equipo) {
            titulo = "VICTORIA";
            color = Color.GOLD;
        } else {
            titulo = "DERROTA";
            color = Color.SCARLET;
        }
        medidor.setText(fuenteEnorme, titulo);
        dibujarTitulo(medidor.width > ANCHO - 30 ? fuenteTitulo : fuenteEnorme, titulo, ANCHO / 2f, 258, color);

        fuente.setColor(Color.LIGHT_GRAY);
        String motivo = estadoActual.motivoFin.equals("PLAYERS")
            ? (todosContraTodos ? "No quedaron rivales" : "Un equipo se quedo sin jugadores")
            : "Tiempo agotado";
        dibujarCentrado(fuente, estadoActual.getModo().nombreVisible + "  -  " + motivo, ANCHO / 2f, 206);

        if (!todosContraTodos) {
            fuente.setColor(Color.SKY);
            dibujarCentrado(fuente, "Azul " + estadoActual.puntosAzul, ANCHO / 2f - 60, 182);
            fuente.setColor(Color.WHITE);
            dibujarCentrado(fuente, "-", ANCHO / 2f, 182);
            fuente.setColor(Color.SALMON);
            dibujarCentrado(fuente, estadoActual.puntosRojo + " Rojo", ANCHO / 2f + 60, 182);
        } else {
            fuente.setColor(Color.WHITE);
            dibujarCentrado(fuente, "Ranking por eliminaciones", ANCHO / 2f, 182);
        }

        // Tabla: en Todos contra todos, ordenada por eliminaciones
        List<EstadoMundo.EstadoJugador> filas = new ArrayList<>(estadoActual.jugadores);
        if (todosContraTodos) {
            filas.sort((x, y) -> x.eliminaciones != y.eliminaciones ? Integer.compare(y.eliminaciones, x.eliminaciones)
                : Integer.compare(x.muertes, y.muertes));
        } else {
            filas.sort((x, y) -> Integer.compare(x.equipo, y.equipo));
        }
        fuente.setColor(Color.GRAY);
        fuente.draw(batch, "Jugador", 70, 145);
        fuente.draw(batch, "Personaje", 160, 145);
        fuente.draw(batch, todosContraTodos ? "Puesto" : "Equipo", 235, 145);
        fuente.draw(batch, "Elim.", 300, 145);
        fuente.draw(batch, "Muertes", 350, 145);
        float y = 128;
        for (EstadoMundo.EstadoJugador p : filas) {
            fuente.setColor(p.id == conexion.getJugadorId() ? Color.YELLOW : Color.WHITE);
            dibujarAjustado(fuente, p.nombre, 70, y, 86);
            fuente.setColor(KitUi.colorPersonaje(TipoPersonaje.desdeId(p.personaje)));
            fuente.draw(batch, TipoPersonaje.desdeId(p.personaje).nombreVisible, 160, y);
            fuente.setColor(KitUi.colorJugador(estadoActual.getModo(), p.equipo));
            // Puesto: 1 + cuántos tienen más eliminaciones (los empatados comparten puesto)
            int puesto = 1;
            for (EstadoMundo.EstadoJugador o : filas) {
                if (o.eliminaciones > p.eliminaciones) puesto++;
            }
            fuente.draw(batch, todosContraTodos ? "#" + puesto : Equipo.nombre(p.equipo), 235, y);
            fuente.setColor(Color.WHITE);
            fuente.draw(batch, String.valueOf(p.eliminaciones), 310, y);
            fuente.draw(batch, String.valueOf(p.muertes), 370, y);
            y -= 15;
        }

        fuente.setColor(Color.GRAY);
        dibujarCentrado(fuente, "Volviendo a la sala en " + (int) Math.ceil(estadoActual.tiempoFase) + " s", ANCHO / 2f, 60);
        dibujarTextoBoton(botonSalir, "Salir al menu");
    }
}
