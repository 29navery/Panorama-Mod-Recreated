package dev.nicho.panoramamod;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class PanoramaManagerTest {
    @TempDir Path temporary;

    @Test
    void captureErrorsAreNeverReportedAsSaved() {
        assertTrue(PanoramaManager.captureSucceeded(net.minecraft.network.chat.Component.translatable("screenshot.success", "folder")));
        assertFalse(PanoramaManager.captureSucceeded(net.minecraft.network.chat.Component.translatable("screenshot.failure", "GPU fence")));
        assertFalse(PanoramaManager.captureSucceeded(net.minecraft.network.chat.Component.literal("error")));
    }

    @BeforeAll
    static void initializeMinecraftVersion() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void loadsNestedCapturesNewestFirstAndIgnoresUnrelatedFiles() throws IOException {
        Path root = temporary.resolve("captures");
        Path older = root.resolve("2026-01-01_old");
        Path newer = root.resolve("2026-02-01_new");
        makeCapture(older, "Old", 6);
        makeCapture(newer, "New", 6);
        Files.createDirectories(root.resolve("invalid"));
        Files.writeString(root.resolve("invalid/panorama_backup.png"), "not a face");
        List<PanoramaManager.PanoramaCapture> captures = PanoramaManager.loadCaptures(root);
        assertEquals(List.of("New", "Old"), captures.stream().map(PanoramaManager.PanoramaCapture::name).toList());
        assertEquals("panorama_0.png", captures.getFirst().previewImage().getFileName().toString());
        assertTrue(PanoramaManager.loadCaptures(temporary.resolve("missing")).isEmpty());
    }

    @Test
    void renamePreservesImagesAndUsesDefaultForBlankNames() throws IOException {
        Path root = temporary.resolve("captures");
        var capture = makeCapture(root.resolve("one"), "Before", 6);
        PanoramaManager.renamePanorama(capture, "  After  ");
        assertEquals("After", PanoramaManager.loadCaptures(root).getFirst().name());
        assertTrue(Files.exists(capture.previewImage()));
        PanoramaManager.renamePanorama(capture, "   ");
        assertEquals(PanoramaManager.defaultCaptureName(), PanoramaManager.loadCaptures(root).getFirst().name());
    }

    @Test
    void exportsSixFacesAndMetadataAcceptedByMinecraft26_3() throws IOException {
        String name = "My \"panorama\" \\ snow\nline\t☃";
        var capture = makeCapture(temporary.resolve("capture"), name, 6);
        Path packs = temporary.resolve("resourcepacks");
        String fileName = PanoramaManager.exportPanorama(packs, capture);
        try (ZipFile zip = new ZipFile(packs.resolve(fileName).toFile())) {
            assertEquals(8, zip.size());
            for (int face = 0; face < 6; face++) {
                var entry = zip.getEntry("assets/minecraft/textures/gui/title/background/panorama_" + face + ".png");
                assertNotNull(entry);
                assertArrayEquals(new byte[]{(byte) face}, zip.getInputStream(entry).readAllBytes());
            }
            assertNotNull(zip.getEntry("pack.png"));
            String json = new String(zip.getInputStream(zip.getEntry("pack.mcmeta")).readAllBytes(), StandardCharsets.UTF_8);
            var pack = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("pack");
            var decoded = PackMetadataSection.CLIENT_TYPE.codec().parse(JsonOps.INSTANCE, pack).getOrThrow();
            assertEquals("Panorama: " + name, decoded.description().getString());
            assertTrue(decoded.supportedFormats().isValueInRange(SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES)));
        }
    }

    @Test
    void repeatedExportsUseSafeDistinctFileNames() throws IOException {
        var capture = makeCapture(temporary.resolve("capture"), "A:/?*<>| panorama.", 6);
        Path packs = temporary.resolve("resourcepacks");
        String first = PanoramaManager.exportPanorama(packs, capture);
        String second = PanoramaManager.exportPanorama(packs, capture);
        assertFalse(first.matches(".*[<>:\"/\\\\|?*].*"));
        assertNotEquals(first, second);
        assertTrue(second.endsWith(" (2).zip"));
        assertTrue(Files.exists(packs.resolve(first)));
    }

    @Test
    void incompleteAndDuplicateFaceSetsDoNotProducePacks() throws IOException {
        var incomplete = makeCapture(temporary.resolve("incomplete"), "Missing", 5);
        Path packs = temporary.resolve("resourcepacks");
        assertThrows(IOException.class, () -> PanoramaManager.exportPanorama(packs, incomplete));
        assertFalse(Files.exists(packs));
        Files.createDirectories(incomplete.directory().resolve("duplicate"));
        Files.write(incomplete.directory().resolve("duplicate/panorama_0.png"), new byte[]{0});
        assertThrows(IOException.class, () -> PanoramaManager.exportPanorama(packs, incomplete));
        assertFalse(Files.exists(packs));
    }

    @Test
    void recursiveDeleteLeavesOtherCapturesUntouched() throws IOException {
        Path root = temporary.resolve("captures");
        var deleted = makeCapture(root.resolve("deleted"), "Deleted", 6);
        var retained = makeCapture(root.resolve("retained"), "Retained", 6);
        PanoramaManager.deletePanorama(deleted);
        assertFalse(Files.exists(deleted.directory()));
        assertTrue(Files.exists(retained.previewImage()));
    }

    private PanoramaManager.PanoramaCapture makeCapture(Path directory, String name, int faces) throws IOException {
        Path images = directory.resolve("nested");
        Files.createDirectories(images);
        Files.writeString(directory.resolve("panorama_name.txt"), name);
        for (int face = 0; face < faces; face++) Files.write(images.resolve("panorama_" + face + ".png"), new byte[]{(byte) face});
        return new PanoramaManager.PanoramaCapture(name, directory.getFileName().toString(), directory, images.resolve("panorama_0.png"));
    }
}
