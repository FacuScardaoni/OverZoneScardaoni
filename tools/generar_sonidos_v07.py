"""
Genera los sonidos de las habilidades de Iris y Phantom:
  assets/heal.wav  Campo de Regeneración (acorde brillante que sube)
  assets/camo.wav  Camuflaje Óptico (barrido que baja y se apaga)
Uso: python tools/generar_sonidos_v07.py      Requiere: pip install numpy
"""
import os
import wave

import numpy as np

RATE = 44100
ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets")


def save(name, signal):
    signal = signal / np.max(np.abs(signal)) * 0.7
    data = (signal * 32767).astype(np.int16)
    with wave.open(os.path.join(ASSETS, name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())
    print("generado:", name)


def heal():
    t = np.linspace(0, 0.6, int(RATE * 0.6), endpoint=False)
    sig = np.zeros_like(t)
    # Arpegio: tres notas que entran una después de otra (do, mi, sol)
    for i, f in enumerate((523.25, 659.25, 783.99)):
        start = i * 0.07
        tt = np.clip(t - start, 0, None)
        env = (t >= start) * np.minimum(1, tt / 0.02) * np.exp(-tt * 5)
        sig += env * (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(4 * np.pi * f * tt))
    sig += 0.15 * np.sin(2 * np.pi * 2093 * t) * np.exp(-t * 9)   # brillo
    return sig


def camo():
    t = np.linspace(0, 0.5, int(RATE * 0.5), endpoint=False)
    freq = 900 * np.exp(-t * 4) + 120                                # baja
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    noise = np.random.default_rng(3).normal(0, 1, len(t))
    noise = np.convolve(noise, np.ones(30) / 30, mode="same")        # ruido suave
    env = np.minimum(1, t / 0.01) * np.exp(-t * 6)
    return env * (0.7 * np.sin(phase) + 0.25 * np.sin(1.5 * phase) + 0.6 * noise)


if __name__ == "__main__":
    save("heal.wav", heal())
    save("camo.wav", camo())
