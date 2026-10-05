package dev.nicho.panoramamod;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class PanoramaBrowserScreen
extends Screen {
    private static final RenderPipeline PREVIEW_PIPELINE = RenderPipelines.GUI_TEXTURED;
    private static final int TILE_SIZE = 118;
    private static final int TILE_GAP = 18;
    private final Screen parent;
    private final List<Tile> tiles = new ArrayList<Tile>();
    private List<PanoramaManager.PanoramaCapture> captures = List.of();
    private int page;
    private int columns;
    private int tilesPerPage;
    private int tileSize;

    public PanoramaBrowserScreen(Screen parent) {
        super(Component.translatable("screen.panoramamod.title"));
        this.parent = parent;
    }

    protected void init() {
        this.captures = PanoramaManager.loadCaptures(this.minecraft);
        int footerY = this.height - 28;
        int buttonWidth = Math.max(40, (this.width - 102) / 3);
        int x = 20;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose()).bounds(x, footerY, buttonWidth, 20).build());
        x += buttonWidth + 6;
        this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.folder"), button -> PanoramaManager.openPanoramaFolder(this.minecraft)).bounds(x, footerY, buttonWidth, 20).build());
        x += buttonWidth + 6;
        this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.refresh"), button -> this.refreshCaptures()).bounds(x, footerY, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1)).bounds(this.width - 66, footerY, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1)).bounds(this.width - 40, footerY, 20, 20).build());
        this.tileSize = Math.min(TILE_SIZE, Math.max(48, this.height - 140));
        this.columns = Math.max(1, Math.min(3, (this.width - 40 + TILE_GAP) / (this.tileSize + TILE_GAP)));
        int rows = Math.max(1, Math.min(2, (this.height - 98 + TILE_GAP) / (this.tileSize + 42 + TILE_GAP)));
        this.tilesPerPage = this.columns * rows;
        this.page = Math.min(this.page, Math.max(0, (this.captures.size() - 1) / this.tilesPerPage));
        this.rebuildTiles();
    }

    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    public void removed() {
        this.destroyTiles();
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
        guiGraphics.text(this.font, this.title, 20, 20, 0xFFFFFFFF, true);
        guiGraphics.text(this.font, Component.translatable("screen.panoramamod.subtitle_grid"), 20, 36, 0xFFAFAFAF, true);
        if (this.tiles.isEmpty()) {
            guiGraphics.text(this.font, Component.translatable("screen.panoramamod.empty"), 20, 64, 0xFFAFAFAF, true);
            return;
        }
        for (Tile tile : this.tiles) {
            this.drawTile(guiGraphics, tile);
        }
    }

    private void drawTile(GuiGraphicsExtractor guiGraphics, Tile tile) {
        int size = this.tileSize;
        guiGraphics.fill(tile.x, tile.y, tile.x + size, tile.y + size, 0xFF1A1A1A);
        int border = tile.button.isHoveredOrFocused() ? 0xFFFFFF00 : 0xFFFFFFFF;
        guiGraphics.fill(tile.x, tile.y, tile.x + size, tile.y + 1, border);
        guiGraphics.fill(tile.x, tile.y + size - 1, tile.x + size, tile.y + size, border);
        guiGraphics.fill(tile.x, tile.y, tile.x + 1, tile.y + size, border);
        guiGraphics.fill(tile.x + size - 1, tile.y, tile.x + size, tile.y + size, border);
        if (tile.textureId != null) {
            int previewSize = size - 8;
            guiGraphics.blit(PREVIEW_PIPELINE, tile.textureId, tile.x + 4, tile.y + 4, 0.0f, 0.0f, previewSize, previewSize, previewSize, previewSize);
        } else {
            guiGraphics.text(this.font, Component.translatable("screen.panoramamod.preview_failed"), tile.x + 8, tile.y + 10, 0xFFFF8080, true);
        }
        String name = this.trimToWidth(tile.capture.name(), size);
        String folder = this.trimToWidth(tile.capture.folderName(), size);
        this.drawCenteredText(guiGraphics, name, tile.x, tile.y + size + 8, size, 0xFFFFFFFF);
        this.drawCenteredText(guiGraphics, folder, tile.x, tile.y + size + 20, size, 0xFFAFAFAF);
        this.drawCenteredText(guiGraphics, Component.translatable("screen.panoramamod.tile_hint"), tile.x, tile.y + size + 32, size, 0xFF7F7F7F);
    }

    private void refreshCaptures() {
        this.captures = PanoramaManager.loadCaptures(this.minecraft);
        this.page = 0;
        this.rebuildTiles();
    }

    private void changePage(int direction) {
        int maxPage = Math.max(0, (this.captures.size() - 1) / this.tilesPerPage);
        this.page = Math.max(0, Math.min(maxPage, this.page + direction));
        this.rebuildTiles();
    }

    private void rebuildTiles() {
        this.destroyTiles();
        int startIndex = this.page * this.tilesPerPage;
        int endIndex = Math.min(startIndex + this.tilesPerPage, this.captures.size());
        int gridX = 20;
        int gridY = 62;
        for (int index = startIndex; index < endIndex; ++index) {
            int localIndex = index - startIndex;
            int column = localIndex % this.columns;
            int row = localIndex / this.columns;
            int x = gridX + column * (this.tileSize + TILE_GAP);
            int y = gridY + row * (this.tileSize + 42 + TILE_GAP);
            PanoramaManager.PanoramaCapture capture = this.captures.get(index);
            Button button = this.addRenderableWidget(Button.builder(Component.literal(capture.name()), widget -> this.openCaptureOptions(capture)).bounds(x, y, this.tileSize, this.tileSize).build());
            Identifier textureId = this.loadTileTexture(capture);
            this.tiles.add(new Tile(capture, button, textureId, x, y));
        }
    }

    private void openCaptureOptions(PanoramaManager.PanoramaCapture capture) {
        this.minecraft.setScreenAndShow(new PanoramaOptionsScreen(this.parent, capture));
    }

    private Identifier loadTileTexture(PanoramaManager.PanoramaCapture capture) {
        try (InputStream input = Files.newInputStream(capture.previewImage())) {
            NativeImage image = NativeImage.read(input);
            DynamicTexture texture = new DynamicTexture(() -> "Panorama preview", image);
            Identifier textureId = Identifier.fromNamespaceAndPath("panoramamod", "preview/" + java.util.UUID.nameUUIDFromBytes(capture.directory().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            this.minecraft.getTextureManager().register(textureId, texture);
            return textureId;
        } catch (IOException exception) {
            PanoramaMod.LOGGER.warn("Failed to load preview for {}", capture.directory(), exception);
            return null;
        }
    }

    private void destroyTiles() {
        for (Tile tile : this.tiles) {
            this.removeWidget(tile.button);
            if (tile.textureId == null) continue;
            this.minecraft.getTextureManager().release(tile.textureId);
        }
        this.tiles.clear();
    }

    private void drawCenteredText(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int width, int color) {
        guiGraphics.text(this.font, Component.literal(text), x + (width - this.font.width(text)) / 2, y, color, true);
    }

    private void drawCenteredText(GuiGraphicsExtractor guiGraphics, Component text, int x, int y, int width, int color) {
        guiGraphics.text(this.font, text, x + (width - this.font.width(text)) / 2, y, color, true);
    }

    private String trimToWidth(String value, int width) {
        if (this.font.width(value) <= width) {
            return value;
        }
        String suffix = "...";
        String trimmed = value;
        while (!trimmed.isEmpty() && this.font.width(trimmed + suffix) > width) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? suffix : trimmed + suffix;
    }

    private record Tile(PanoramaManager.PanoramaCapture capture, Button button, Identifier textureId, int x, int y) {
    }
}
