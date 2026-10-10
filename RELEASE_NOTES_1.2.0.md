Fixes panorama capture crashing on Minecraft 26.3 with `Cannot wait on a fence for the current submit`, followed by `PreparedFrame already in use`.

- Submit GPU work between panorama faces so renderer buffers can be reused safely.
- Show a capture failure message when Minecraft returns an error, instead of claiming the panorama was saved.

Requires Minecraft 26.3, Java 25, Fabric Loader 0.19.5 or newer, and Fabric API. Install `panoramamod-1.2.0+mc26.3.jar`; the sources JAR is for developers.

Validation: client sources compiled, seven regression tests passed, and the capture mixin was checked against Minecraft's actual bytecode. Live capture with Iris and shader packs still needs in-game verification.
