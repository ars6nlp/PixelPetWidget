"""Sounds of the aquarium, synthesised (no recordings): short, soft and in the same 8-bit-ish style
as the other app sounds (22050 Hz, 16-bit mono WAV in app/src/main/res/raw).

    python tools/sound_gen/aquarium_sounds.py

voice_aquarium_1.wav   "blup": a bubble, a quick upward chirp
voice_aquarium_2.wav   "plop": a lower, rounder drop into the water
sfx_aquarium_pick.wav  calm bubbling when the aquarium is chosen
sfx_aquarium_drop.wav  a soft rising droplet for good news

Every sound is a few sine "bubbles": a tone gliding up (a real bubble's pitch rises as it
shrinks) under a fast-in, smooth-out envelope. Change the numbers in SOUNDS and run again.
"""
import math
import os
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'res', 'raw')
PEAK = 9000             # about as loud as the other app sounds (they peak at 6500-13000)


def bubble(start_hz, end_hz, ms, level=1.0, wobble=0.0):
    """One bubble: a sine sliding from start_hz to end_hz, with a little vibrato on top."""
    n = int(RATE * ms / 1000)
    out = []
    phase = 0.0
    for i in range(n):
        k = i / n
        freq = start_hz + (end_hz - start_hz) * (k ** 0.6)
        freq *= 1 + wobble * math.sin(2 * math.pi * 30 * i / RATE)
        phase += 2 * math.pi * freq / RATE
        envelope = min(1.0, i / (RATE * 0.004)) * (1 - k) ** 2
        out.append(level * envelope * math.sin(phase))
    return out


def silence(ms):
    return [0.0] * int(RATE * ms / 1000)


def mix(parts):
    """Bubbles one after another; (offset ms, samples) pairs may overlap."""
    length = max(int(RATE * off / 1000) + len(s) for off, s in parts)
    out = [0.0] * length
    for off, s in parts:
        start = int(RATE * off / 1000)
        for i, v in enumerate(s):
            out[start + i] += v
    return out


SOUNDS = {
    'voice_aquarium_1': lambda: mix([(0, bubble(380, 900, 90)), (70, bubble(700, 1300, 50, 0.5))]),
    'voice_aquarium_2': lambda: mix([(0, bubble(220, 520, 130, wobble=0.02)), (110, bubble(480, 760, 60, 0.4))]),
    'sfx_aquarium_pick': lambda: mix([(0, bubble(300, 620, 80, 0.8)), (90, bubble(420, 780, 70, 0.6)),
                                      (170, bubble(340, 700, 80, 0.7)), (260, bubble(500, 900, 60, 0.5))]),
    'sfx_aquarium_drop': lambda: mix([(0, bubble(600, 1200, 110)), (120, bubble(900, 1700, 140, 0.55))]),
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
