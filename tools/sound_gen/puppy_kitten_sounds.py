"""Voices of the puppy and the kitten, synthesised (no recordings): short, soft and in the same
8-bit-ish style as the other app sounds (22050 Hz, 16-bit mono WAV in app/src/main/res/raw).
They play in the app only (a tap on the pet in the preview); the widget makes no sound.

    python tools/sound_gen/puppy_kitten_sounds.py

voice_puppy_1.wav    "yip": one bright little bark, the pitch jumping up and dropping
voice_puppy_2.wav    "yip-yip": two quicker, slightly higher ones
voice_kitten_1.wav   "mew": a thin glide up and down with a soft vibrato
voice_kitten_2.wav   purr: a low hum, pulsing about 25 times a second

Every sound is a tone (a sine with a quieter second harmonic for a little body) whose pitch
follows a curve, under a soft attack and a smooth fade. Change the numbers in SOUNDS and run again.
"""
import math
import os
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'res', 'raw')
PEAK = 7000             # soft: at the quiet end of the other app voices (6500-13000)


def tone(pitch, ms, level=1.0, harmonic=0.25, attack_ms=6, vibrato=0.0, vibrato_hz=7.0, pulse_hz=0.0):
    """A tone whose frequency is pitch(k) for k going 0..1 over ms; vibrato wobbles the pitch,
    pulse_hz beats the loudness (a purr)."""
    n = int(RATE * ms / 1000)
    out = []
    phase = 0.0
    for i in range(n):
        k = i / n
        freq = pitch(k) * (1 + vibrato * math.sin(2 * math.pi * vibrato_hz * i / RATE))
        phase += 2 * math.pi * freq / RATE
        envelope = min(1.0, i / (RATE * attack_ms / 1000)) * (1 - k) ** 1.5
        if pulse_hz:
            envelope *= 0.55 + 0.45 * math.sin(2 * math.pi * pulse_hz * i / RATE)
        out.append(level * envelope * (math.sin(phase) + harmonic * math.sin(2 * phase)))
    return out


def glide(*points):
    """A pitch curve through (k, hz) points, eased between them."""
    def pitch(k):
        for (k0, f0), (k1, f1) in zip(points, points[1:]):
            if k <= k1:
                t = (k - k0) / (k1 - k0) if k1 > k0 else 0
                t = t * t * (3 - 2 * t)
                return f0 + (f1 - f0) * t
        return points[-1][1]
    return pitch


def silence(ms):
    return [0.0] * int(RATE * ms / 1000)


def mix(parts):
    """Tones one after another; (offset ms, samples) pairs may overlap."""
    length = max(int(RATE * off / 1000) + len(s) for off, s in parts)
    out = [0.0] * length
    for off, s in parts:
        start = int(RATE * off / 1000)
        for i, v in enumerate(s):
            out[start + i] += v
    return out


def yip(base, ms):
    """One little bark: up quickly, then down."""
    return tone(glide((0, base * 0.8), (0.25, base * 1.35), (1, base * 0.7)), ms, harmonic=0.35, attack_ms=4)


SOUNDS = {
    'voice_puppy_1': lambda: mix([(0, yip(620, 100))]),
    'voice_puppy_2': lambda: mix([(0, yip(680, 75)), (100, yip(720, 80))]),
    'voice_kitten_1': lambda: mix([(0, tone(glide((0, 760), (0.35, 1080), (1, 640)), 220, harmonic=0.18, attack_ms=25,
                                            vibrato=0.015))]),
    'voice_kitten_2': lambda: mix([(0, tone(glide((0, 120), (1, 105)), 300, level=0.9, harmonic=0.5, attack_ms=50,
                                            pulse_hz=25))]),
}


def write(name, samples):
    peak = max(abs(v) for v in samples) or 1.0
    data = b''.join(struct.pack('<h', int(v / peak * PEAK)) for v in samples)
    with wave.open(os.path.join(OUT, f'{name}.wav'), 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data)


def main():
    for name, make in SOUNDS.items():
        samples = make() + silence(20)
        write(name, samples)
        print(f'{name}.wav  {len(samples) / RATE * 1000:.0f} ms')


if __name__ == '__main__':
    main()
