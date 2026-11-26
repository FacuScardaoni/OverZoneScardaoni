import os
import wave

import numpy as np

FRECUENCIA_MUESTREO = 44100
RECURSOS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")

def guardar(nombre, senal_total):
    senal_total = senal_total / np.max(np.abs(senal_total)) * 0.7
    datos = (senal_total * 32767).astype(np.int16)
    with wave.open(os.path.join(RECURSOS, nombre), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(FRECUENCIA_MUESTREO)
        w.writeframes(datos.tobytes())
    print("generado:", nombre)

def curacion():
    t = np.linspace(0, 0.6, int(FRECUENCIA_MUESTREO * 0.6), endpoint=False)
    senal = np.zeros_like(t)
    # Arpegio: tres notas que entran una después de otra (do, mi, sol)
    for i, f in enumerate((523.25, 659.25, 783.99)):
        inicio = i * 0.07
        tt = np.clip(t - inicio, 0, None)
        envolvente = (t >= inicio) * np.minimum(1, tt / 0.02) * np.exp(-tt * 5)
        senal += envolvente * (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(4 * np.pi * f * tt))
    senal += 0.15 * np.sin(2 * np.pi * 2093 * t) * np.exp(-t * 9)   # brillo
    return senal

def camuflaje():
    t = np.linspace(0, 0.5, int(FRECUENCIA_MUESTREO * 0.5), endpoint=False)
    frecuencia = 900 * np.exp(-t * 4) + 120                                # baja
    fase = 2 * np.pi * np.cumsum(frecuencia) / FRECUENCIA_MUESTREO
    ruido = np.random.default_rng(3).normal(0, 1, len(t))
    ruido = np.convolve(ruido, np.ones(30) / 30, mode="same")        # ruido suave
    envolvente = np.minimum(1, t / 0.01) * np.exp(-t * 6)
    return envolvente * (0.7 * np.sin(fase) + 0.25 * np.sin(1.5 * fase) + 0.6 * ruido)

if __name__ == "__main__":
    guardar("heal.wav", curacion())
    guardar("camo.wav", camuflaje())
