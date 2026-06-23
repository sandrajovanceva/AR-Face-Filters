"""Find the bounding box of the yellow tassel parts in graduation_cap.glb."""
import json
import struct
import sys

import numpy as np

path = sys.argv[1] if len(sys.argv) > 1 else "app/src/main/assets/models/graduation_cap.glb"
with open(path, "rb") as f:
    data = f.read()
chunk_len = struct.unpack_from("<I", data, 12)[0]
gltf = json.loads(data[20:20 + chunk_len])
bin_off = 20 + chunk_len + 8


def positions(acc_idx):
    acc = gltf["accessors"][acc_idx]
    bv = gltf["bufferViews"][acc["bufferView"]]
    return np.frombuffer(data, np.float32, acc["count"] * 3,
                         bin_off + bv.get("byteOffset", 0)).reshape(-1, 3)


# overall bounds
allp = np.vstack([positions(p["attributes"]["POSITION"])
                  for m in gltf["meshes"] for p in m["primitives"]])
print("WHOLE CAP  min", allp.min(0), "max", allp.max(0))

# per-primitive, flag yellowish materials (tassel)
for m in gltf["meshes"]:
    for p in m["primitives"]:
        mat = gltf["materials"][p["material"]]
        col = mat.get("pbrMetallicRoughness", {}).get("baseColorFactor", [0, 0, 0, 1])
        r, g, b = col[:3]
        is_yellow = r > 0.5 and g > 0.4 and b < 0.4
        pos = positions(p["attributes"]["POSITION"])
        tag = "TASSEL?" if is_yellow else "       "
        print(f"{tag} color=({r:.2f},{g:.2f},{b:.2f})  "
              f"min={np.round(pos.min(0), 4)} max={np.round(pos.max(0), 4)}")
