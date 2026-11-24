"""
Genera assets/shield.wav: el sonido del Escudo Cinético (un zumbido grave
que sube, con un brillo metálico). Uso: python tools/generar_sonido_escudo.py
Requiere: pip install numpy
"""
import os
import wave

import numpy as np

RATE = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "shield.wav")

t = np.linspace(0, 0.45, int(RATE * 0.45), endpoint=False)
freq = 110 + 220 * (t / t[-1]) ** 0.6                 # tono que sube
phase = 2 * np.pi * np.cumsum(freq) / RATE
hum = 0.6 * np.sin(phase) + 0.25 * np.sin(2 * phase) + 0.12 * np.sin(5.03 * phase)
shine = 0.15 * np.sin(2 * np.pi * 1760 * t) * np.exp(-t * 18)
envelope = np.minimum(1, t / 0.03) * np.exp(-t * 4.5)
signal = (hum + shine) * envelope
signal = signal / np.max(np.abs(signal)) * 0.7
data = (signal * 32767).astype(np.int16)

with wave.open(OUT, "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(RATE)
    w.writeframes(data.tobytes())
print("generado:", OUT)
