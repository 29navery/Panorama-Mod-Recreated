package dev.nicho.panoramamod;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.mojang.blaze3d.Blaze3D;

public final class PanoramaManager {
    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int EXPECTED_PANORAMA_FACES = 6;
    private static final String NAME_FILE = "panorama_name.txt";
    private static boolean captureQueued;
    private static String queuedCaptureName;

    private PanoramaManager() {
    }

    public static boolean canCapture(Minecraft client) {
        return client.level != null && client.player != null;
    }

    public static boolean requestCapture(Minecraft client, String requestedName) {
        if (!PanoramaManager.canCapture(client)) {
            return false;
        }
        captureQueued = true;
        queuedCaptureName = PanoramaManager.normalizeCaptureName(requestedName);
        return true;
    }

    public static void tick(Minecraft client) {
        if (!captureQueued || client.level == null || client.player == null) {
            return;
        }
        captureQueued = false;
        PanoramaManager.capturePanorama(client);
    }

    public static void sendClientMessage(Minecraft client, Component text) {
        if (client.player != null) {
            client.player.sendSystemMessage(text);
        }
    }

    private static void capturePanorama(Minecraft client) {
        String captureName = queuedCaptureName;
        try {
            Path outputDirectory = PanoramaManager.ensurePanoramaRoot(client).resolve(FOLDER_FORMAT.format(LocalDateTime.now()) + "_" + PanoramaManager.slugify(captureName));
            Files.createDirectories(outputDirectory);
            Files.writeString(outputDirectory.resolve(NAME_FILE), captureName);
            Component vanillaMessage = client.grabPanoramixScreenshot(outputDirectory.toFile());
            if (!captureSucceeded(vanillaMessage)) {
                PanoramaMod.LOGGER.error("Panorama capture failed: {}", vanillaMessage.getString());
                PanoramaManager.sendClientMessage(client, Component.translatable("message.panoramamod.capture_failed")
                    .withStyle(ChatFormatting.RED).append(Component.literal(" ")).append(vanillaMessage.copy()));
                return;
            }
            MutableComponent message = Component.translatable("message.panoramamod.capture_saved", new Object[]{captureName}).withStyle(ChatFormatting.GREEN).append(Component.literal(" ")).append(vanillaMessage.copy()).append(Component.literal(" ")).append(Component.translatable("message.panoramamod.capture_browse").withStyle(ChatFormatting.GRAY));
            PanoramaManager.sendClientMessage(client, message);
        }
        catch (Exception exception) {
            PanoramaMod.LOGGER.error("Failed to create panorama capture", exception);
            PanoramaManager.sendClientMessage(client, Component.translatable("message.panoramamod.capture_failed").withStyle(ChatFormatting.RED));
        }
    }

    static boolean captureSucceeded(Component result) {
        return result.getContents() instanceof TranslatableContents translation
            && "screenshot.success".equals(translation.getKey());
    }

    private static Path getPanoramaRoot(Minecraft client) {
        return client.gameDirectory.toPath().resolve("mods").resolve(".panorama");
    }

    private static Path ensurePanoramaRoot(Minecraft client) throws IOException {
        Path root = PanoramaManager.getPanoramaRoot(client);
        Files.createDirectories(root);
        PanoramaManager.hidePanoramaRoot(root);
        return root;
    }

    private static void hidePanoramaRoot(Path root) {
        try {
            Files.setAttribute(root, "dos:hidden", true);
        }
        catch (IOException | IllegalArgumentException | SecurityException | UnsupportedOperationException exception) {
            PanoramaMod.LOGGER.debug("Could not apply hidden attribute to panorama folder {}", root, exception);
        }
    }

    public static List<PanoramaCapture> loadCaptures(Minecraft client) {
        return loadCaptures(getPanoramaRoot(client));
    }

    static List<PanoramaCapture> loadCaptures(Path root) {
        ArrayList<PanoramaCapture> captures = new ArrayList<PanoramaCapture>();
        if (!Files.isDirectory(root)) {
            return captures;
        }
        PanoramaManager.hidePanoramaRoot(root);
        try (Stream<Path> directories = Files.list(root);){
            directories.filter(x$0 -> Files.isDirectory(x$0)).sorted(Comparator.reverseOrder()).forEach(directory -> {
                try {
                    Path previewImage = PanoramaManager.findPreviewImage(directory);
                    if (previewImage != null) {
                        captures.add(new PanoramaCapture(PanoramaManager.readCaptureName(directory), directory.getFileName().toString(), directory, previewImage));
                    }
                }
                catch (IOException exception) {
                    PanoramaMod.LOGGER.warn("Skipping invalid panorama capture at {}", directory, exception);
                }
            });
        }
        catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to load panorama captures", exception);
        }
        return captures;
    }

    public static String exportPanorama(Minecraft client, PanoramaCapture capture) throws IOException {
        return exportPanorama(client.getResourcePackDirectory(), capture);
    }

    static String exportPanorama(Path resourcePackDir, PanoramaCapture capture) throws IOException {
        List<Path> panoramaFiles = PanoramaManager.listPanoramaFiles(capture.directory());
        if (panoramaFiles.size() != EXPECTED_PANORAMA_FACES) {
            throw new IOException("Expected 6 panorama faces, found " + panoramaFiles.size());
        }
        for (int face = 0; face < EXPECTED_PANORAMA_FACES; face++) {
            String expectedName = "panorama_" + face + ".png";
            if (panoramaFiles.stream().noneMatch(path -> path.getFileName().toString().equals(expectedName))) {
                throw new IOException("Missing panorama face " + expectedName);
            }
        }
        Files.createDirectories(resourcePackDir);
        String packFileName = PanoramaManager.nextPackFileName(resourcePackDir, capture.name());
        Path packZip = resourcePackDir.resolve(packFileName);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(packZip));){
            PanoramaManager.writeZipText(output, "pack.mcmeta", PanoramaManager.buildPackDescription(capture.name()));
            PanoramaManager.writeZipFile(output, "pack.png", capture.previewImage());
            for (Path source : panoramaFiles) {
                PanoramaManager.writeZipFile(output, "assets/minecraft/textures/gui/title/background/" + String.valueOf(source.getFileName()), source);
            }
        }
        return packFileName;
    }

    private static List<Path> listPanoramaFiles(Path captureDirectory) throws IOException {
        ArrayList<Path> panoramaFiles = new ArrayList<Path>();
        if (!Files.isDirectory(captureDirectory)) {
            return panoramaFiles;
        }
        try (Stream<Path> files = Files.walk(captureDirectory, 4);){
            files.filter(path -> {
                String name = path.getFileName().toString();
                return Files.isRegularFile(path) && name.matches("panorama_[0-5]\\.png");
            }).sorted().forEach(panoramaFiles::add);
        }
        return panoramaFiles;
    }

    private static Path findPreviewImage(Path captureDirectory) throws IOException {
        List<Path> panoramaFiles = PanoramaManager.listPanoramaFiles(captureDirectory);
        for (Path path : panoramaFiles) {
            if (!path.getFileName().toString().equals("panorama_0.png")) continue;
            return path;
        }
        return panoramaFiles.isEmpty() ? null : panoramaFiles.getFirst();
    }

    private static String readCaptureName(Path captureDirectory) throws IOException {
        String value;
        Path nameFile = captureDirectory.resolve(NAME_FILE);
        if (Files.isRegularFile(nameFile) && !(value = Files.readString(nameFile).trim()).isEmpty()) {
            return value;
        }
        return captureDirectory.getFileName().toString();
    }

    private static String buildPackDescription(String captureName) {
        PackFormat format = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
        JsonObject pack = new JsonObject();
        pack.addProperty("description", "Panorama: " + captureName);
        // Since resource pack format 65, Minecraft uses min/max format ranges.
        com.google.gson.JsonArray version = new com.google.gson.JsonArray();
        version.add(format.major());
        version.add(format.minor());
        pack.add("min_format", version);
        pack.add("max_format", version.deepCopy());
        JsonObject metadata = new JsonObject();
        metadata.add("pack", pack);
        return metadata.toString();
    }

    private static String nextPackFileName(Path resourcePackDir, String captureName) {
        String baseName = PanoramaManager.safeFileName(PanoramaManager.normalizeCaptureName(captureName));
        String candidate = baseName + ".zip";
        int suffix = 2;
        while (Files.exists(resourcePackDir.resolve(candidate))) {
            candidate = baseName + " (" + suffix + ").zip";
            ++suffix;
        }
        return candidate;
    }

    private static String normalizeCaptureName(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? PanoramaManager.defaultCaptureName() : trimmed;
    }

    private static String slugify(String value) {
        String slug = PanoramaManager.normalizeCaptureName(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "panorama" : slug;
    }

    private static String safeFileName(String value) {
        String safeName = value.replaceAll("[<>:\"/\\\\|?*]+", "_").replaceAll("[\\p{Cntrl}]+", "").replaceAll("\\s+", " ").trim().replaceAll("[. ]+$", "");
        return safeName.isEmpty() ? "Panorama" : safeName;
    }

    private static void writeZipText(ZipOutputStream output, String name, String content) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        output.write(content.getBytes(StandardCharsets.UTF_8));
        output.closeEntry();
    }

    private static void writeZipFile(ZipOutputStream output, String name, Path source) throws IOException {
        output.putNextEntry(new ZipEntry(name));
        Files.copy(source, output);
        output.closeEntry();
    }

    public static void renamePanorama(PanoramaCapture capture, String requestedName) throws IOException {
        Files.writeString(capture.directory().resolve(NAME_FILE), PanoramaManager.normalizeCaptureName(requestedName));
    }

    public static void deletePanorama(PanoramaCapture capture) throws IOException {
        try (Stream<Path> paths = Files.walk(capture.directory());){
            List<Path> files = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : files) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static String defaultCaptureName() {
        return "My Panorama";
    }

    public static void openPanoramaFolder(Minecraft client) {
        try {
            Path root = PanoramaManager.ensurePanoramaRoot(client);
            Blaze3D.openPath(root);
        }
        catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to open panorama folder", exception);
            PanoramaManager.sendClientMessage(client, Component.translatable("message.panoramamod.open_failed").withStyle(ChatFormatting.RED));
        }
    }

    static {
        queuedCaptureName = PanoramaManager.defaultCaptureName();
    }

    public record PanoramaCapture(String name, String folderName, Path directory, Path previewImage) {
    }
}
