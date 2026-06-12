"""Render quick previews of the filter .glb models (painter's algorithm).

    python tools/preview_glb.py          # renders all models to tools/preview/
"""
import json
import math
import os
import struct

import numpy as np
from PIL import Image, ImageDraw

MODELS_DIR = os.path.join("app", "src", "main", "assets", "models")
OUT_DIR = os.path.join("tools", "preview")
SIZE = 380
YAW = math.radians(25)
PITCH = math.radians(-12)


def load_glb(path):
    with open(path, "rb") as f:
        data = f.read()
    chunk_len = struct.unpack_from("<I", data, 12)[0]
    gltf = json.loads(data[20:20 + chunk_len])
    bin_off = 20 + chunk_len + 8
    tris = []  # (v0, v1, v2, color)
    for mesh in gltf["meshes"]:
        for prim in mesh["primitives"]:
            acc = gltf["accessors"][prim["attributes"]["POSITION"]]
            bv = gltf["bufferViews"][acc["bufferView"]]
            pos = np.frombuffer(
                data, np.float32, acc["count"] * 3,
                bin_off + bv.get("byteOffset", 0)).reshape(-1, 3)
            acc_i = gltf["accessors"][prim["indices"]]
            bv_i = gltf["bufferViews"][acc_i["bufferView"]]
            idx = np.frombuffer(data, np.uint16, acc_i["count"],
                                bin_off + bv_i.get("byteOffset", 0))
            mat = gltf["materials"][prim["material"]]
            color = mat["pbrMetallicRoughness"]["baseColorFactor"][:3]
            tris.append((pos, idx.reshape(-1, 3), np.array(color)))
    return tris


def render(path, out_path):
    prims = load_glb(path)
    cy, sy = math.cos(YAW), math.sin(YAW)
    cp, sp = math.cos(PITCH), math.sin(PITCH)
    rot_y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rot_x = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    m = rot_x @ rot_y

    all_v = np.vstack([p for p, _, _ in prims]) @ m.T
    lo, hi = all_v.min(axis=0), all_v.max(axis=0)
    center = (lo + hi) / 2
    scale = (SIZE * 0.42) / max((hi - lo).max() / 2, 1e-6)

    light = np.array([0.4, 0.6, 0.7])
    light = light / np.linalg.norm(light)

    faces = []
    for pos, idx, color in prims:
        v = pos @ m.T
        t = v[idx]  # (n, 3, 3)
        n = np.cross(t[:, 1] - t[:, 0], t[:, 2] - t[:, 0])
        ln = np.linalg.norm(n, axis=1, keepdims=True)
        ln[ln == 0] = 1
        n = n / ln
        shade = 0.25 + 0.75 * np.abs(n @ light)
        depth = t[:, :, 2].mean(axis=1)
        for k in range(len(t)):
            faces.append((depth[k], t[k], color * shade[k]))

    faces.sort(key=lambda f: f[0])
    img = Image.new("RGB", (SIZE, SIZE), (24, 24, 28))
    draw = ImageDraw.Draw(img)
    for _, t, c in faces:
        pts = [(SIZE / 2 + (p[0] - center[0]) * scale,
                SIZE / 2 - (p[1] - center[1]) * scale) for p in t]
        draw.polygon(pts, fill=tuple(int(min(255, x * 255)) for x in c))
    img.save(out_path)
    print("rendered", out_path)


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    for name in sorted(os.listdir(MODELS_DIR)):
        if name.endswith(".glb"):
            render(os.path.join(MODELS_DIR, name),
                   os.path.join(OUT_DIR, name.replace(".glb", ".png")))
