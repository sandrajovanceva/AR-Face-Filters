"""Generate a proper graduation mortarboard .glb.

Mortarboard = low, shallow skullcap that tucks under a FLAT SQUARE board +
centre button + a tassel that runs from the button to a SIDE corner and hangs
down the side. Reuses helpers from generate_filter_models.

    PYTHONPATH=tools python tools/generate_graduation_cap.py
"""
import math
import os

import generate_filter_models as g

OUT = os.path.join("app", "src", "main", "assets", "models", "graduation_cap.glb")

CLOTH = (0.07, 0.07, 0.08, 1.0)
CLOTH_HI = (0.11, 0.11, 0.13, 1.0)
BUTTON = (0.05, 0.05, 0.05, 1.0)
TASSEL = (1.00, 0.80, 0.16, 1.0)
TASSEL_HI = (1.00, 0.86, 0.30, 1.0)


def box(hx, hy, hz):
    faces = [
        ((0, 0, 1), [(-hx, -hy, hz), (hx, -hy, hz), (hx, hy, hz), (-hx, hy, hz)]),
        ((0, 0, -1), [(hx, -hy, -hz), (-hx, -hy, -hz), (-hx, hy, -hz), (hx, hy, -hz)]),
        ((1, 0, 0), [(hx, -hy, hz), (hx, -hy, -hz), (hx, hy, -hz), (hx, hy, hz)]),
        ((-1, 0, 0), [(-hx, -hy, -hz), (-hx, -hy, hz), (-hx, hy, hz), (-hx, hy, -hz)]),
        ((0, 1, 0), [(-hx, hy, hz), (hx, hy, hz), (hx, hy, -hz), (-hx, hy, -hz)]),
        ((0, -1, 0), [(-hx, -hy, -hz), (hx, -hy, -hz), (hx, -hy, hz), (-hx, -hy, hz)]),
    ]
    verts, idx = [], []
    for n, quad in faces:
        s = len(verts)
        for p in quad:
            verts.append((p[0], p[1], p[2], n[0], n[1], n[2]))
        idx += [s, s + 1, s + 2, s, s + 2, s + 3]
    return verts, idx


CYL = g.cylinder(caps=True)
SPH = g.sphere()

# Low, shallow skullcap that tucks under the board (radius, y) in metres.
CAP_PROFILE = [
    (0.067, 0.000),
    (0.066, 0.012),
    (0.063, 0.022),
    (0.055, 0.029),
    (0.035, 0.033),
    (0.000, 0.034),
]
CAP_TOP = 0.034
BOARD_Y = CAP_TOP + 0.001
BOARD_HW = 0.090
BOARD_HT = 0.0035

parts = []
parts.append(g.part(g.lathe(CAP_PROFILE), color=CLOTH, roughness=0.85))
parts.append(g.part(box(BOARD_HW, BOARD_HT, BOARD_HW),
                    rot=(0, 45, 0), trans=(0, BOARD_Y, 0),
                    color=CLOTH_HI, roughness=0.8))

BTN_Y = BOARD_Y + BOARD_HT
parts.append(g.part(CYL, (0.011, 0.004, 0.011), trans=(0, BTN_Y + 0.004, 0),
                    color=BUTTON, roughness=0.6))
parts.append(g.part(SPH, (0.011, 0.006, 0.011), trans=(0, BTN_Y + 0.008, 0),
                    color=BUTTON, roughness=0.55))

# Tassel anchored at the RIGHT-side corner (+X) so it hangs down the side.
CX = BOARD_HW * math.sqrt(2.0) - 0.004
CORNER = (CX, BOARD_Y + BOARD_HT, 0.0)

# cord lying flat across the board from the button out to the side corner
cord_path = g.bezier((0.0, BTN_Y + 0.004, 0.0),
                     (CX * 0.4, BTN_Y + 0.006, 0.0),
                     (CX * 0.8, CORNER[1] + 0.004, 0.0),
                     (CX, CORNER[1] + 0.003, 0.0), 16)
parts.append(g.part(g.tube(cord_path, [0.0022, 0.0022, 0.0022]),
                    color=TASSEL, metallic=0.1, roughness=0.45))

KNOT = (CORNER[0] + 0.004, CORNER[1] - 0.006, 0.0)
parts.append(g.part(CYL, (0.006, 0.010, 0.006), trans=KNOT,
                    color=TASSEL_HI, metallic=0.1, roughness=0.4))

N_STRANDS = 14
STRAND_LEN = 0.072
top_y = KNOT[1] - 0.009
for k in range(N_STRANDS):
    a = 2.0 * math.pi * k / N_STRANDS
    spread = 0.006
    sx = math.cos(a) * spread
    sz = math.sin(a) * spread
    bottom = (KNOT[0] + sx * 1.8 + 0.004, top_y - STRAND_LEN, KNOT[2] + sz * 1.8)
    sp = g.bezier((KNOT[0] + sx * 0.3, top_y, KNOT[2] + sz * 0.3),
                  (KNOT[0] + sx, top_y - STRAND_LEN * 0.4, KNOT[2] + sz),
                  bottom, bottom, 8)
    parts.append(g.part(g.tube(sp, [0.0013, 0.0013, 0.0011], sectors=8),
                        color=TASSEL if k % 2 else TASSEL_HI,
                        metallic=0.1, roughness=0.45))

parts.append(g.part(SPH, (0.011, 0.013, 0.011),
                    trans=(KNOT[0] + 0.004, top_y - STRAND_LEN + 0.002, KNOT[2]),
                    color=TASSEL_HI, metallic=0.1, roughness=0.4))

if __name__ == "__main__":
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    g.write_glb(OUT, parts)
