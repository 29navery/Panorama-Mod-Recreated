package dev.nicho.panoramamod;

import dev.nicho.panoramamod.PanoramaMod;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
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
import net.minecraft.util.Util;

public final class PanoramaManager {
    private static final DateTimeFormatter FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int EXPECTED_PANORAMA_FACES = 6;
    private static final int PACK_FORMAT = 89;
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
            Files.createDirectories(outputDirectory, new FileAttribute[0]);
            Files.writeString(outputDirectory.resolve(NAME_FILE), captureName, new OpenOption[0]);
            Component vanillaMessage = client.grabPanoramixScreenshot(outputDirectory.toFile());
            MutableComponent message = Component.translatable("message.panoramamod.capture_saved", captureName)
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" "))
                .append(vanillaMessage.copy())
                .append(Component.literal(" "))
                .append(Component.translatable("message.panoramamod.capture_browse").withStyle(ChatFormatting.GRAY));
            PanoramaManager.sendClientMessage(client, message);
        } catch (Exception exception) {
            PanoramaMod.LOGGER.error("Failed to create panorama capture", exception);
            PanoramaManager.sendClientMessage(client, Component.translatable("message.panoramamod.capture_failed").withStyle(ChatFormatting.RED));
        }
    }

    private static Path getPanoramaRoot(Minecraft client) {
        return client.gameDirectory.toPath().resolve("mods").resolve(".panorama");
    }

    private static Path ensurePanoramaRoot(Minecraft client) throws IOException {
        Path root = PanoramaManager.getPanoramaRoot(client);
        Files.createDirectories(root, new FileAttribute[0]);
        PanoramaManager.hidePanoramaRoot(root);
        return root;
    }

    private static void hidePanoramaRoot(Path root) {
        try {
            Files.setAttribute(root, "dos:hidden", true, new LinkOption[0]);
        } catch (IOException | IllegalArgumentException | SecurityException | UnsupportedOperationException exception) {
            PanoramaMod.LOGGER.debug("Could not apply hidden attribute to panorama folder {}", root, exception);
        }
    }

    public static List<PanoramaCapture> loadCaptures(Minecraft client) {
        Path root = PanoramaManager.getPanoramaRoot(client);
        ArrayList<PanoramaCapture> captures = new ArrayList<>();
        if (!Files.isDirectory(root, new LinkOption[0])) {
            return captures;
        }
        PanoramaManager.hidePanoramaRoot(root);
        try (Stream<Path> directories = Files.list(root)) {
            directories.filter(x -> Files.isDirectory(x, new LinkOption[0]))
                .sorted(Comparator.reverseOrder())
                .forEach(directory -> {
                    try {
                        Path previewImage = PanoramaManager.findPreviewImage(directory);
                        if (previewImage != null) {
                            captures.add(new PanoramaCapture(PanoramaManager.readCaptureName(directory), directory.getFileName().toString(), directory, previewImage));
                        }
                    } catch (IOException exception) {
                        PanoramaMod.LOGGER.warn("Skipping invalid panorama capture at {}", directory, exception);
                    }
                });
        } catch (IOException exception) {
            PanoramaMod.LOGGER.error("Failed to load panorama captures", exception);
        }
        return captures;
    }

    public static String exportPanorama(Minecraft client, PanoramaCapture capture) throws IOException {
        List<Path> panoramaFiles = PanoramaManager.listPanoramaFiles(capture.directory());
        if (panoramaFiles.size() < 6) {
            throw new IOException("Expected 6 panorama faces, found " + panoramaFiles.size());
        }
        Path resourcePackDir = client.gameDirectory.toPath().resolve("resourcepacks");
        Files.createDirectories(resourcePackDir, new FileAttribute[0]);
        String packFileName = PanoramaManager.nextPackFileName(resourcePackDir, capture.name());
        Path packZip = resourcePackDir.resolve(packFileName);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(packZip, new OpenOption[0]))) {
            PanoramaManager.writeZipText(output, "pack.mcmeta", PanoramaManager.buildPackDescription(capture.name()));
            PanoramaManager.writeZipFile(output, "pack.png", capture.previewImage());
            for (Path source : panoramaFiles) {
                PanoramaManager.writeZipFile(output, "assets/minecraft/textures/gui/title/background/" + source.getFileName(), source);
            }
        }
        return packFileName;
    }

    private static List<Path> listPanoramaFiles(Path captureDirectory) throws IOException {
        ArrayList<Path> panoramaFiles = new ArrayList<>();
        if (!Files.isDirectory(captureDirectory, new LinkOption[0])) {
            return panoramaFiles;
        }
        try (Stream<Path> files = Files.walk(captureDirectory, 4, new FileVisitOption[0])) {
            files.filter(path -> {
                String name = path.getFileName().toString();
                return name.startsWith("panorama_") && name.endsWith(".png");
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
        if (Files.isRegularFile(nameFile, new LinkOption[0]) && !(value = Files.readString(nameFile).trim()).isEmpty()) {
            return value;
        }
        return captureDirectory.getFileName().toString();
    }

    private static String buildPackDescription(String captureName) {
        return "{\n  \"pack\": {\n    \"pack_format\": %d,\n    \"description\": \"%s\"\n  }\n}\n".formatted(89, "Panorama: " + PanoramaManager.escapeJson(captureName));
    }

    private static String nextPackFileName(Path resourcePackDir, String captureName) {
        String baseName = PanoramaManager.safeFileName(PanoramaManager.normalizeCaptureName(captureName));
        String candidate = baseName + ".zip";
        int suffix = 2;
        while (Files.exists(resourcePackDir.resolve(candidate), new LinkOption[0])) {
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

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
        Files.writeString(capture.directory().resolve(NAME_FILE), PanoramaManager.normalizeCaptureName(requestedName), new OpenOption[0]);
    }

    public static void deletePanorama(PanoramaCapture capture) throws IOException {
        try (Stream<Path> paths = Files.walk(capture.directory(), new FileVisitOption[0])) {
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
            Util.getPlatform().open(root.toUri());
        } catch (IOException exception) {
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