package dev.nicho.panoramamod;

import com.mojang.blaze3d.platform.NativeImage;
import dev.nicho.panoramamod.PanoramaManager;
import dev.nicho.panoramamod.PanoramaMod;
import dev.nicho.panoramamod.PanoramaOptionsScreen;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;

public final class PanoramaBrowserScreen extends Screen {
    private static final int TILES_PER_PAGE = 6;
    private static final int TILE_SIZE = 118;
    private static final int TILE_GAP = 18;
    private final Screen parent;
    private final List<Tile> tiles = new ArrayList<>();
    private List<PanoramaManager.PanoramaCapture> captures = List.of();
    private int page;

    public PanoramaBrowserScreen(Screen parent) {
        super(Component.translatable("screen.panoramamod.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.captures = PanoramaManager.loadCaptures(this.minecraft);
        int footerY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose()).bounds(20, footerY, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.folder"), button -> PanoramaManager.openPanoramaFolder(this.minecraft)).bounds(110, footerY, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("button.panoramamod.refresh"), button -> this.refreshCaptures()).bounds(220, footerY, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.changePage(-1)).bounds(this.width - 110, footerY, 20, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.changePage(1)).bounds(this.width - 80, footerY, 20, 20).build());
        this.rebuildTiles();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public void removed() {
        this.destroyTiles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        this.extractTransparentBackground(guiGraphics);
        super.extractRenderState(guiGraphics, mouseX, mouseY, deltaTicks);
        guiGraphics.text(this.font, this.title, 20, 20, 0xFFFFFF, true);
        guiGraphics.text(this.font, Component.translatable("screen.panoramamod.subtitle_grid"), 20, 36, 0xAFAFAF, true);
        if (this.tiles.isEmpty()) {
            guiGraphics.text(this.font, Component.translatable("screen.panoramamod.empty"), 20, 64, 0xAFAFAF, true);
            return;
        }
        for (Tile tile : this.tiles) {
            this.drawTile(guiGraphics, tile);
        }
    }

    private void drawTile(GuiGraphicsExtractor guiGraphics, Tile tile) {
        guiGraphics.fill(tile.x, tile.y, tile.x + 118, tile.y + 118, -15066598);
        guiGraphics.fill(tile.x, tile.y, tile.x + 118, tile.y + 1, -1);
        guiGraphics.fill(tile.x, tile.y + 118 - 1, tile.x + 118, tile.y + 118, -1);
        guiGraphics.fill(tile.x, tile.y, tile.x + 1, tile.y + 118, -1);
        guiGraphics.fill(tile.x + 118 - 1, tile.y, tile.x + 118, tile.y + 118, -1);
        if (tile.textureId != null) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, tile.textureId, tile.x + 4, tile.y + 4, 0.0f, 0.0f, 110, 110, 110, 110);
        } else {
            guiGraphics.text(this.font, Component.translatable("screen.panoramamod.preview_failed"), tile.x + 8, tile.y + 10, 0xFF8080, true);
        }
        String name = this.trimToWidth(tile.capture.name(), 118);
        String folder = this.trimToWidth(tile.capture.folderName(), 118);
        this.drawCenteredText(guiGraphics, name, tile.x, tile.y + 118 + 8, 118, 0xFFFFFF);
        this.drawCenteredText(guiGraphics, folder, tile.x, tile.y + 118 + 20, 118, 0xAFAFAF);
        this.drawCenteredText(guiGraphics, Component.translatable("screen.panoramamod.tile_hint"), tile.x, tile.y + 118 + 32, 118, 0x7F7F7F);
    }

    private void refreshCaptures() {
        this.captures = PanoramaManager.loadCaptures(this.minecraft);
        this.page = 0;
        this.rebuildTiles();
    }

    private void changePage(int direction) {
        int maxPage = Math.max(0, (this.captures.size() - 1) / 6);
        this.page = Math.max(0, Math.min(maxPage, this.page + direction));
        this.rebuildTiles();
    }

    private void rebuildTiles() {
        this.destroyTiles();
        int startIndex = this.page * 6;
        int endIndex = Math.min(startIndex + 6, this.captures.size());
        int gridX = 20;
        int gridY = 62;
        for (int index = startIndex; index < endIndex; ++index) {
            int localIndex = index - startIndex;
            int column = localIndex % 3;
            int row = localIndex / 3;
            int x = gridX + column * 136;
            int y = gridY + row * 176;
            PanoramaManager.PanoramaCapture capture = this.captures.get(index);
            Button button = this.addRenderableWidget(Button.builder(Component.empty(), widget -> this.openCaptureOptions(capture)).bounds(x, y, 118, 118).build());
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
            texture.upload();
            Identifier identifier = Identifier.fromNamespaceAndPath("panoramamod", "preview/" + capture.folderName());
            this.minecraft.getTextureManager().register(identifier, texture);
            return identifier;
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