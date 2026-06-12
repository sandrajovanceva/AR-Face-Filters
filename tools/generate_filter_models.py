"""Generate the filter .glb models — swept/lathed surfaces authored in meters.

Coordinate convention matches ARCore Augmented Faces region poses:
+Y up, +Z out of the face toward the camera. Run from the repo root:

    python tools/generate_filter_models.py
"""
import json
import math
import os
import struct

OUT_DIR = os.path.join("app", "src", "main", "assets", "models")


# ------------------------------------------------------------- vector helpers

def v_sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def v_add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def v_scale(a, s):
    return (a[0] * s, a[1] * s, a[2] * s)


def v_dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def v_cross(a, b):
    return (a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0])


def v_norm(a):
    ln = math.sqrt(v_dot(a, a)) or 1.0
    return (a[0] / ln, a[1] / ln, a[2] / ln)


def bezier(p0, p1, p2, p3, samples):
    pts = []
    for i in range(samples + 1):
        t = i / samples
        u = 1.0 - t
        pts.append(tuple(
            u ** 3 * p0[k] + 3 * u * u * t * p1[k]
            + 3 * u * t * t * p2[k] + t ** 3 * p3[k]
            for k in range(3)))
    return pts


def lerp_profile(values, t):
    """Piecewise-linear interpolation over [0,1] of a list of values."""
    if t <= 0:
        return values[0]
    if t >= 1:
        return values[-1]
    f = t * (len(values) - 1)
    i = int(f)
    return values[i] + (values[i + 1] - values[i]) * (f - i)


# ---------------------------------------------------------------- primitives
# Each primitive returns (verts, indices); verts = (px,py,pz, nx,ny,nz).


def sphere(sectors=28, stacks=18):
    verts, idx = [], []
    for i in range(stacks + 1):
        phi = math.pi * i / stacks
        y, r = math.cos(phi), math.sin(phi)
        for j in range(sectors + 1):
            t = 2.0 * math.pi * j / sectors
            x, z = r * math.cos(t), r * math.sin(t)
            verts.append((x, y, z, x, y, z))
    for i in range(stacks):
        for j in range(sectors):
            a = i * (sectors + 1) + j
            b = a + sectors + 1
            idx += [a, b, a + 1, a + 1, b, b + 1]
    return verts, idx


def cone(sectors=28):
    """Base circle r=1 at y=0, apex at (0,1,0), with base cap."""
    verts, idx = [], []
    inv = 1.0 / math.sqrt(2.0)
    for j in range(sectors + 1):
        t = 2.0 * math.pi * j / sectors
        c, s = math.cos(t), math.sin(t)
        verts.append((c, 0.0, s, c * inv, inv, s * inv))
    apex_start = len(verts)
    for j in range(sectors):
        t = 2.0 * math.pi * (j + 0.5) / sectors
        c, s = math.cos(t), math.sin(t)
        verts.append((0.0, 1.0, 0.0, c * inv, inv, s * inv))
    for j in range(sectors):
        idx += [j, apex_start + j, j + 1]
    cap_start = len(verts)
    for j in range(sectors + 1):
        t = 2.0 * math.pi * j / sectors
        verts.append((math.cos(t), 0.0, math.sin(t), 0.0, -1.0, 0.0))
    center = len(verts)
    verts.append((0.0, 0.0, 0.0, 0.0, -1.0, 0.0))
    for j in range(sectors):
        idx += [center, cap_start + j, cap_start + j + 1]
    return verts, idx


def cylinder(sectors=28, caps=True):
    """Radius 1, y from -0.5 to 0.5."""
    verts, idx = [], []
    for y in (-0.5, 0.5):
        for j in range(sectors + 1):
            t = 2.0 * math.pi * j / sectors
            c, s = math.cos(t), math.sin(t)
            verts.append((c, y, s, c, 0.0, s))
    for j in range(sectors):
        a, b = j, j + sectors + 1
        idx += [a, b, a + 1, a + 1, b, b + 1]
    if caps:
        for y, ny in ((-0.5, -1.0), (0.5, 1.0)):
            start = len(verts)
            for j in range(sectors + 1):
                t = 2.0 * math.pi * j / sectors
                verts.append((math.cos(t), y, math.sin(t), 0.0, ny, 0.0))
            center = len(verts)
            verts.append((0.0, y, 0.0, 0.0, ny, 0.0))
            for j in range(sectors):
                idx += [center, start + j, start + j + 1]
    return verts, idx


def lathe(profile, sectors=28, arc=(0.0, 2.0 * math.pi)):
    """Revolve a 2D profile [(radius, y), ...] around the Y axis.

    Open surface (no caps) — with doubleSided materials this gives a
    hollow shell. A partial `arc` (e.g. π..2π) gives an open-fronted
    concave shell like a real ear.
    """
    verts, idx = [], []
    n = len(profile)
    a0, a1 = arc
    for i, (r, y) in enumerate(profile):
        # 2D outward normal from the profile tangent
        ra, ya = profile[max(0, i - 1)]
        rb, yb = profile[min(n - 1, i + 1)]
        dr, dy = rb - ra, yb - ya
        ln = math.sqrt(dr * dr + dy * dy) or 1.0
        nr, ny = dy / ln, -dr / ln
        for j in range(sectors + 1):
            t = a0 + (a1 - a0) * j / sectors
            c, s = math.cos(t), math.sin(t)
            verts.append((r * c, y, r * s, nr * c, ny, nr * s))
    for i in range(n - 1):
        for j in range(sectors):
            a = i * (sectors + 1) + j
            b = a + sectors + 1
            idx += [a, b, a + 1, a + 1, b, b + 1]
    return verts, idx


def torus(ring_radius, tube_radius, y=0.0, segments=24):
    """Torus around the Y axis, expressed as a lathe of a circle profile."""
    profile = []
    for i in range(segments + 1):
        a = 2.0 * math.pi * i / segments
        profile.append((ring_radius + tube_radius * math.cos(a),
                        y + tube_radius * math.sin(a)))
    return lathe(profile)


def tube(path, radii, sectors=20, rx=1.0, rz=1.0, cap_start=True, cap_end=True):
    """Sweep an elliptical cross-section along a 3D path.

    Frames are parallel-transported to avoid twisting. `radii` is a
    radius profile interpolated along the path. rx scales the
    cross-section along the transported normal, rz along the binormal.
    """
    n = len(path)
    tangents = [v_norm(v_sub(path[min(n - 1, i + 1)], path[max(0, i - 1)]))
                for i in range(n)]
    ref = (0.0, 0.0, 1.0) if abs(tangents[0][2]) < 0.9 else (1.0, 0.0, 0.0)
    normals = [v_norm(v_cross(v_cross(tangents[0], ref), tangents[0]))]
    for i in range(1, n):
        prev, t = normals[-1], tangents[i]
        normals.append(v_norm(v_sub(prev, v_scale(t, v_dot(prev, t)))))
    binormals = [v_cross(tangents[i], normals[i]) for i in range(n)]

    verts, idx = [], []
    for i, p in enumerate(path):
        r = lerp_profile(radii, i / (n - 1))
        for j in range(sectors + 1):
            t = 2.0 * math.pi * j / sectors
            c, s = math.cos(t), math.sin(t)
            off = v_add(v_scale(normals[i], r * rx * c),
                        v_scale(binormals[i], r * rz * s))
            nrm = v_norm(v_add(v_scale(normals[i], c / rx),
                               v_scale(binormals[i], s / rz)))
            verts.append((*v_add(p, off), *nrm))
    for i in range(n - 1):
        for j in range(sectors):
            a = i * (sectors + 1) + j
            b = a + sectors + 1
            idx += [a, b, a + 1, a + 1, b, b + 1]
    for at_end in (False, True):
        if at_end and not cap_end:
            continue
        if not at_end and not cap_start:
            continue
        i = n - 1 if at_end else 0
        nrm = tangents[i] if at_end else v_scale(tangents[i], -1.0)
        start = i * (sectors + 1)
        center = len(verts)
        verts.append((*path[i], *nrm))
        for j in range(sectors):
            if at_end:
                idx += [center, start + j, start + j + 1]
            else:
                idx += [center, start + j + 1, start + j]
    return verts, idx


# ---------------------------------------------------------------- transforms

def rot_matrix(rx, ry, rz):
    """Euler degrees, applied X then Y then Z."""
    ax, ay, az = (math.radians(v) for v in (rx, ry, rz))
    cx, sx = math.cos(ax), math.sin(ax)
    cy, sy = math.cos(ay), math.sin(ay)
    cz, sz = math.cos(az), math.sin(az)
    m_x = ((1, 0, 0), (0, cx, -sx), (0, sx, cx))
    m_y = ((cy, 0, sy), (0, 1, 0), (-sy, 0, cy))
    m_z = ((cz, -sz, 0), (sz, cz, 0), (0, 0, 1))

    def mul(a, b):
        return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3))
                           for j in range(3)) for i in range(3))
    return mul(m_z, mul(m_y, m_x))


def m_apply(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def part(prim, scale=(1, 1, 1), rot=(0, 0, 0), trans=(0, 0, 0),
         color=(1, 1, 1, 1), metallic=0.0, roughness=0.7):
    """Bake scale → rotation → translation into a primitive's vertices."""
    verts, idx = prim
    m = rot_matrix(*rot)
    out = []
    for px, py, pz, nx, ny, nz in verts:
        p = m_apply(m, (px * scale[0], py * scale[1], pz * scale[2]))
        # inverse-transpose for normals (handles non-uniform and mirror scale)
        n = m_apply(m, (nx / scale[0], ny / scale[1], nz / scale[2]))
        ln = math.sqrt(n[0] ** 2 + n[1] ** 2 + n[2] ** 2) or 1.0
        out.append((p[0] + trans[0], p[1] + trans[1], p[2] + trans[2],
                    n[0] / ln, n[1] / ln, n[2] / ln))
    return out, idx, color, metallic, roughness


def mirror_x(parts):
    """Mirror a list of parts across the YZ plane (left ↔ right ear etc.)."""
    out = []
    for verts, idx, color, metallic, roughness in parts:
        mv = [(-px, py, pz, -nx, ny, nz) for px, py, pz, nx, ny, nz in verts]
        out.append((mv, idx, color, metallic, roughness))
    return out


# ---------------------------------------------------------------- glb writer

def write_glb(path, parts):
    bin_blob = b""
    buffer_views, accessors, primitives, materials = [], [], [], []

    def add_view(data, target):
        nonlocal bin_blob
        bin_blob += b"\0" * (-len(bin_blob) % 4)
        buffer_views.append({"buffer": 0, "byteOffset": len(bin_blob),
                             "byteLength": len(data), "target": target})
        bin_blob += data
        return len(buffer_views) - 1

    for verts, idx, rgba, metallic, roughness in parts:
        positions = [v[:3] for v in verts]
        pos_data = b"".join(struct.pack("<3f", *v[:3]) for v in verts)
        nrm_data = b"".join(struct.pack("<3f", *v[3:]) for v in verts)
        idx_data = b"".join(struct.pack("<H", i) for i in idx)

        pos_acc = len(accessors)
        accessors.append({
            "bufferView": add_view(pos_data, 34962), "componentType": 5126,
            "count": len(positions), "type": "VEC3",
            "min": [min(p[k] for p in positions) for k in range(3)],
            "max": [max(p[k] for p in positions) for k in range(3)],
        })
        accessors.append({"bufferView": add_view(nrm_data, 34962),
                          "componentType": 5126, "count": len(verts), "type": "VEC3"})
        accessors.append({"bufferView": add_view(idx_data, 34963),
                          "componentType": 5123, "count": len(idx), "type": "SCALAR"})
        materials.append({
            "pbrMetallicRoughness": {"baseColorFactor": list(rgba),
                                     "metallicFactor": metallic,
                                     "roughnessFactor": roughness},
            "doubleSided": True,
        })
        primitives.append({"attributes": {"POSITION": pos_acc, "NORMAL": pos_acc + 1},
                           "indices": pos_acc + 2, "material": len(materials) - 1})

    gltf = {
        "asset": {"version": "2.0", "generator": "arfilterapp-model-gen"},
        "scene": 0, "scenes": [{"nodes": [0]}], "nodes": [{"mesh": 0}],
        "meshes": [{"primitives": primitives}],
        "materials": materials, "accessors": accessors,
        "bufferViews": buffer_views, "buffers": [{"byteLength": len(bin_blob)}],
    }
    js = json.dumps(gltf, separators=(",", ":")).encode()
    js += b" " * (-len(js) % 4)
    bin_blob += b"\0" * (-len(bin_blob) % 4)
    total = 12 + 8 + len(js) + 8 + len(bin_blob)
    with open(path, "wb") as f:
        f.write(struct.pack("<III", 0x46546C67, 2, total))
        f.write(struct.pack("<II", len(js), 0x4E4F534A) + js)
        f.write(struct.pack("<II", len(bin_blob), 0x004E4942) + bin_blob)
    print(f"wrote {path} ({total} bytes)")


# ------------------------------------------------------------------- models

SPHERE = sphere()
CYL_OPEN = cylinder(caps=False)

BROWN = (0.36, 0.22, 0.10, 1.0)
DOG_NOSE = (0.10, 0.07, 0.06, 1.0)
CAT_GRAY = (0.35, 0.33, 0.36, 1.0)
PINK = (0.98, 0.62, 0.72, 1.0)
NOSE_PINK = (0.95, 0.45, 0.55, 1.0)
WHISKER = (0.95, 0.95, 0.95, 1.0)
WHITE = (0.96, 0.94, 0.93, 1.0)
GOLD = (1.00, 0.78, 0.28, 1.0)
RUBY = (0.80, 0.05, 0.15, 1.0)
SAPPHIRE = (0.10, 0.20, 0.70, 1.0)
EMERALD = (0.05, 0.50, 0.20, 1.0)
VELVET = (0.45, 0.05, 0.10, 1.0)
DEVIL_RED = (0.55, 0.08, 0.08, 1.0)
CLOWN_RED = (0.85, 0.10, 0.10, 1.0)
DARK_BROWN = (0.16, 0.10, 0.06, 1.0)

# --- Dog: perky upright ear, tip folds slightly outward (swept tube) -------
dog_ear_path = bezier((0.000, 0.000, 0.000), (0.006, 0.030, 0.002),
                      (0.020, 0.052, 0.002), (0.034, 0.048, 0.004), 24)
dog_ear_l = [part(tube(dog_ear_path, [0.006, 0.014, 0.016, 0.010, 0.002],
                       rx=0.45, rz=1.0),
                  color=BROWN, roughness=0.65)]

dog_nose = [part(SPHERE, (0.017, 0.012, 0.010), color=DOG_NOSE, roughness=0.3)]

# --- Cat: open-fronted concave shell ear + pink lining + nose + whiskers ----
HALF = (math.pi, 2.0 * math.pi)  # отворот гледа кон +Z (кон камерата)
EAR_PROFILE = [(1.00, 0.00), (0.93, 0.16), (0.80, 0.36), (0.60, 0.56),
               (0.38, 0.76), (0.18, 0.91), (0.00, 1.00)]
cat_ear_l = [
    part(lathe(EAR_PROFILE, arc=HALF), (0.026, 0.052, 0.012), (0, 0, -18),
         color=CAT_GRAY, roughness=0.6),
    part(lathe(EAR_PROFILE, arc=HALF), (0.018, 0.038, 0.009), (0, 0, -18),
         (0.0, 0.004, 0.0025), color=PINK, roughness=0.55),
]

cat_nose = [part(SPHERE, (0.011, 0.008, 0.007), color=NOSE_PINK, roughness=0.4)]
for side in (-1, 1):
    for k, ang in enumerate((10, 0, -10)):
        cat_nose.append(part(
            CYL_OPEN, (0.0008, 0.070, 0.0008),
            (0, 0, side * (90 + ang)),
            (side * 0.048, 0.002 - k * 0.005, 0.010),
            WHISKER, roughness=0.5))

# --- Bunny: tall open-fronted shell ears with pink lining -------------------
BUNNY_PROFILE = [(0.55, 0.00), (0.80, 0.20), (0.90, 0.45), (0.72, 0.72),
                 (0.40, 0.90), (0.00, 1.00)]
bunny_ear_l = [
    part(lathe(BUNNY_PROFILE, arc=HALF), (0.020, 0.085, 0.009), (0, 0, -10),
         (0.004, 0.008, 0), color=WHITE, roughness=0.6),
    part(lathe(BUNNY_PROFILE, arc=HALF), (0.014, 0.064, 0.006), (0, 0, -10),
         (0.005, 0.012, 0.002), color=PINK, roughness=0.55),
]

# --- Devil: curved glossy horn (swept tube) ---------------------------------
horn_path = bezier((0.000, 0.000, 0.000), (0.002, 0.028, 0.004),
                   (0.014, 0.052, -0.004), (0.028, 0.060, -0.018), 24)
devil_horn_l = [part(tube(horn_path, [0.012, 0.009, 0.006, 0.003, 0.0008]),
                     color=DEVIL_RED, metallic=0.15, roughness=0.25)]

# --- Crown: band + rims + alternating spikes + jewels + velvet dome ---------
crown = [
    part(CYL_OPEN, (0.075, 0.030, 0.075), color=GOLD, metallic=1.0, roughness=0.28),
    part(torus(0.075, 0.004, y=-0.015), color=GOLD, metallic=1.0, roughness=0.28),
    part(torus(0.075, 0.004, y=0.015), color=GOLD, metallic=1.0, roughness=0.28),
    part(lathe([(1.00, 0.00), (0.92, 0.35), (0.70, 0.70), (0.40, 0.90), (0.00, 1.00)]),
         (0.072, 0.033, 0.072), trans=(0, 0.012, 0), color=VELVET, roughness=0.95),
]
CONE = cone()
for k in range(8):
    a = 2.0 * math.pi * k / 8
    height = 0.042 if k % 2 == 0 else 0.026
    crown.append(part(CONE, (0.010, height, 0.010),
                      trans=(0.075 * math.sin(a), 0.015, 0.075 * math.cos(a)),
                      color=GOLD, metallic=1.0, roughness=0.28))
for a_deg, gem in ((0, RUBY), (-60, SAPPHIRE), (60, EMERALD)):
    a = math.radians(a_deg)
    crown.append(part(SPHERE, (0.0065, 0.008, 0.005),
                      trans=(0.0775 * math.sin(a), 0.0, 0.0775 * math.cos(a)),
                      color=gem, metallic=0.1, roughness=0.12))

# --- Clown: glossy nose + curling handlebar mustache (swept tubes) ----------
clown_nose = [part(SPHERE, (0.022, 0.022, 0.022), color=CLOWN_RED, roughness=0.25)]

curl_path = bezier((0.002, -0.004, 0.002), (0.020, -0.010, 0.006),
                   (0.042, -0.002, 0.002), (0.056, 0.016, -0.004), 24)
curl = [part(tube(curl_path, [0.0045, 0.008, 0.0085, 0.006, 0.0015], rz=0.6),
             color=DARK_BROWN, roughness=0.45)]
mustache = curl + mirror_x(curl)

# --- Clown party hat: striped cone with a pompom, tilted playfully ----------
YELLOW = (0.98, 0.80, 0.10, 1.0)
SKY_BLUE = (0.15, 0.45, 0.85, 1.0)
HAT_TILT = (0, 0, 10)
HAT_R, HAT_H, BANDS = 0.052, 0.105, 5
clown_hat = []
for i, col in enumerate((CLOWN_RED, YELLOW, SKY_BLUE, YELLOW, CLOWN_RED)):
    y0, y1 = HAT_H * i / BANDS, HAT_H * (i + 1) / BANDS
    r0, r1 = HAT_R * (1 - i / BANDS), HAT_R * (1 - (i + 1) / BANDS)
    clown_hat.append(part(lathe([(r0, y0), (r1, y1)]), rot=HAT_TILT,
                          color=col, roughness=0.55))
tilt = math.radians(HAT_TILT[2])
clown_hat.append(part(SPHERE, (0.014, 0.014, 0.014),
                      trans=(-math.sin(tilt) * HAT_H, math.cos(tilt) * HAT_H, 0),
                      color=WHITE, roughness=0.85))

MODELS = {
    "dog_ear_l.glb": dog_ear_l,
    "dog_ear_r.glb": mirror_x(dog_ear_l),
    "dog_nose.glb": dog_nose,
    "cat_ear_l.glb": cat_ear_l,
    "cat_ear_r.glb": mirror_x(cat_ear_l),
    "cat_nose.glb": cat_nose,
    "bunny_ear_l.glb": bunny_ear_l,
    "bunny_ear_r.glb": mirror_x(bunny_ear_l),
    "devil_horn_l.glb": devil_horn_l,
    "devil_horn_r.glb": mirror_x(devil_horn_l),
    "crown.glb": crown,
    "clown_nose.glb": clown_nose,
    "mustache.glb": mustache,
    "clown_hat.glb": clown_hat,
}

if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, model_parts in MODELS.items():
        write_glb(os.path.join(OUT_DIR, name), model_parts)
