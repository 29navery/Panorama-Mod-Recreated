# Panorama Mod Recreated

A client-side Fabric mod for Minecraft **26.3**. Capture six panorama faces from your world, browse saved captures, rename or delete them, and export them as title-screen resource packs.

## Install and use

Requires **Java 25**, **Fabric Loader 0.19.5 or newer**, and **Fabric API for Minecraft 26.3**. Put the normal `panoramamod-1.1.0+mc26.3.jar` in your instance's `mods` folder. Do not install the `-sources.jar`. Mod Menu 21.0.0 is optional and adds a shortcut to the settings screen.

- **F6** in a world: name and capture a panorama. Minecraft captures six 4096×4096 images, so this can take a moment.
- **F7**: open Panorama Settings. Both keys can be changed in Minecraft's Controls menu.
- **Panoramas** on the title screen: browse captures and access export, rename, and delete.
- **Export**: create a ZIP in `resourcepacks`. Enable it in Minecraft's Resource Packs screen to use the panorama.

Captures are kept in `<instance>/mods/.panorama`, including captures from the original mod. The Open Folder button opens that directory. The browser adjusts its page size to the window and GUI scale.

## Build

Use a JDK 25 installation:

```sh
./gradlew clean build
```

On Windows, run `gradlew.bat clean build`. The installable mod and source JAR are written to `build/libs/`. GitHub Actions builds each push and uploads both under the **Artifacts** download on the workflow run.

The build runs six regression tests covering capture discovery, renaming, recursive deletion, incomplete/duplicate faces, safe repeated exports, and exported metadata decoded by Minecraft 26.3's actual resource-pack codec. These tests do not launch the graphical client or capture a live world.

## License

CC-BY-NC-4.0; see [LICENSE](LICENSE).
