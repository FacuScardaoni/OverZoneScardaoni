import os
import wave

import numpy as np

FRECUENCIA_MUESTREO = 44100
SALIDA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "shield.wav")

t = np.linspace(0, 0.45, int(FRECUENCIA_MUESTREO * 0.45), endpoint=False)
frecuencia = 110 + 220 * (t / t[-1]) ** 0.6                 # tono que sube
fase = 2 * np.pi * np.cumsum(frecuencia) / FRECUENCIA_MUESTREO
zumbido = 0.6 * np.sin(fase) + 0.25 * np.sin(2 * fase) + 0.12 * np.sin(5.03 * fase)
brillo_agudo = 0.15 * np.sin(2 * np.pi * 1760 * t) * np.exp(-t * 18)
envolvente_total = np.minimum(1, t / 0.03) * np.exp(-t * 4.5)
senal_total = (zumbido + brillo_agudo) * envolvente_total
senal_total = senal_total / np.max(np.abs(senal_total)) * 0.7
datos = (senal_total * 32767).astype(np.int16)

with wave.open(SALIDA, "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(FRECUENCIA_MUESTREO)
    w.writeframes(datos.tobytes())
print("generado:", SALIDA)
