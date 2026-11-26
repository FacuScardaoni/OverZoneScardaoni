package com.OverZone.Scardaoni.pantallas;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.OverZone.Scardaoni.JuegoOverZone;
import com.OverZone.Scardaoni.red.RedCliente;

public class PantallaConexion extends PantallaMenuBase {
    private final Rectangle botonReintentar = new Rectangle(110, 40, 120, 30);
    private final Rectangle botonVolver  = new Rectangle(250, 40, 120, 30);
    private float transcurrido = 0f;

    public PantallaConexion(JuegoOverZone juego) {
        super(juego, 1.6f);
    }

    @Override
    public void show() {
        super.show();
        juego.conectar();
    }

    @Override
    protected InputProcessor crearProcesadorEntrada() {
        return new InputAdapter() {
            @Override
            public boolean keyUp(int codigoTecla) {
                if (codigoTecla == Input.Keys.ESCAPE) {
                    cancelar();
                    return true;
                }
                if (codigoTecla == Input.Keys.ENTER && fallo()) {
                    reintentar();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int pantallaX, int pantallaY, int puntero, int boton) {
                if (boton != Input.Buttons.LEFT) return false;
                if (fallo() && clickeado(botonReintentar, pantallaX, pantallaY)) {
                    reintentar();
                    return true;
                }
                if (clickeado(botonVolver, pantallaX, pantallaY)) {
                    cancelar();
                    return true;
                }
                return false;
            }
        };
    }

    private boolean fallo() {
        RedCliente conexion = juego.getRed();
        return conexion == null || conexion.getEstadoConexion() == RedCliente.EstadoConexion.FALLIDO;
    }

    private void reintentar() {
        transcurrido = 0f;
        juego.conectar();
    }

    private void cancelar() {
        juego.desconectar();
        juego.reemplazarPantalla(new PantallaMenu(juego));
    }

    @Override
    protected boolean actualizar(float delta) {
        transcurrido += delta;
        RedCliente conexion = juego.getRed();
        // Ya conectado y con el primer estado recibido: se pasa a la pantalla
        // que corresponda a la etapa de la partida (sala, juego o resultados).
        if (conexion != null && conexion.getEstadoConexion() == RedCliente.EstadoConexion.CONECTADO
            && conexion.getUltimoEstado() != null) {
            return juego.seguirServidor();
        }
        return false;
    }

    @Override
    protected void dibujarFormas(Vector3 mouse) {
        if (fallo()) dibujarBoton(botonReintentar, mouse, 0.20f, 0.50f, 0.85f);
        dibujarBoton(botonVolver, mouse, 0.45f, 0.30f, 0.30f);
    }

    @Override
    protected void dibujarTexto(Vector3 mouse) {
        RedCliente conexion = juego.getRed();
        dibujarTitulo("Conectando", ANCHO / 2f, 232, Color.WHITE);

        String puntos = ".".repeat(1 + (int) (transcurrido * 3) % 3);
        fuente.setColor(Color.LIGHT_GRAY);
        if (conexion == null) {
            dibujarCentrado(fuente, "Sin conexion", ANCHO / 2f, 170);
        } else {
            switch (conexion.getEstadoConexion()) {
                case BUSCANDO:
                    dibujarCentrado(fuente, "Buscando servidor en la red local" + puntos, ANCHO / 2f, 170);
                    dibujarCentrado(fuente, "(broadcast UDP al puerto 5555)", ANCHO / 2f, 150);
                    break;
                case CONECTANDO:
                case CONECTADO:
                    dibujarCentrado(fuente, "Servidor encontrado: " + conexion.getNombreServidor()
                        + " (" + conexion.getDireccionServidor() + ")", ANCHO / 2f, 170);
                    dibujarCentrado(fuente, "Uniendose a la partida" + puntos, ANCHO / 2f, 150);
                    break;
                case FALLIDO:
                    fuente.setColor(Color.ORANGE);
                    float y = 180;
                    for (String linea : conexion.getMensajeFalla().split("\n")) {
                        dibujarCentrado(fuente, linea, ANCHO / 2f, y);
                        y -= 20;
                    }
                    break;
                default:
                    break;
            }
        }

        if (fallo()) dibujarTextoBoton(botonReintentar, "Reintentar");
        dibujarTextoBoton(botonVolver, "Volver");
    }
}
