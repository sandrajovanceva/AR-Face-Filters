"""Print the JSON chunk of a .glb and the min/max bounds of all POSITION accessors."""
import json
import struct
import sys

path = sys.argv[1]
with open(path, "rb") as f:
    data = f.read()

magic, version, length = struct.unpack_from("<III", data, 0)
assert magic == 0x46546C67, "not a glb"
chunk_len, chunk_type = struct.unpack_from("<II", data, 12)
gltf = json.loads(data[20:20 + chunk_len].decode("utf-8"))

print("meshes:", json.dumps(gltf.get("meshes"), indent=1)[:800])
print("nodes:", json.dumps(gltf.get("nodes"), indent=1)[:800])
for i, acc in enumerate(gltf.get("accessors", [])):
    if "min" in acc and acc.get("type") == "VEC3":
        print(f"accessor {i}: min={acc['min']} max={acc['max']}")
