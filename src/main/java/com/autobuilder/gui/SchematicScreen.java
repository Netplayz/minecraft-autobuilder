package com.autobuilder.gui;

import com.autobuilder.AutoBuilderMod;
import com.autobuilder.builder.BuildJob;
import com.autobuilder.builder.BuildManager;
import com.autobuilder.builder.BuildState;
import com.autobuilder.gather.MaterialRequirement;
import com.autobuilder.schematic.SchematicInfo;
import com.autobuilder.schematic.SchematicManager;
import com.autobuilder.util.BlockCounter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SchematicScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 200;
    private static final int ITEM_HEIGHT = 22;

    private final SchematicManager schematicManager;
    private final BuildManager buildManager;

    private SchematicInfo selectedSchematic;
    private int scrollOffset;
    private int selectedIndex = -1;
    private List<SchematicInfo> schematics;
    private String statusMessage;
    private long statusMessageTime;

    private Button litematicaButton;
    private Button startButton;
    private Button gatherButton;
    private Button cancelButton;
    private Button reloadButton;

    public SchematicScreen() {
        super(Component.literal("AutoBuilder"));
        this.schematicManager = AutoBuilderMod.getInstance().getSchematicManager();
        this.buildManager = AutoBuilderMod.getInstance().getBuildManager();
        this.schematics = schematicManager.getAllSchematics();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int bw = 160;

        litematicaButton = addRenderableWidget(Button.builder(
                Component.literal("\u00a76\u00a7lBuild Litematica Placement"),
                btn -> buildLitematica()
        ).bounds(cx - bw / 2, 30, bw, 24).build());

        cancelButton = addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> cancelBuild()
        ).bounds(cx - bw / 2, 60, bw / 2 - 4, 20).build());

        reloadButton = addRenderableWidget(Button.builder(
                Component.literal("\u00a7aReload Schematics"),
                btn -> reloadSchematics()
        ).bounds(cx + 4, 60, bw / 2 - 4, 20).build());

        int fileSectionTop = 95;
        int fileLabelY = fileSectionTop + 26;
        int fileButtonY = fileLabelY + 4;

        startButton = addRenderableWidget(Button.builder(
                Component.literal("Start Build"),
                btn -> startBuild()
        ).bounds(cx - bw / 2, fileButtonY, bw, 20).build());

        gatherButton = addRenderableWidget(Button.builder(
                Component.literal("Gather Only"),
                btn -> gatherOnly()
        ).bounds(cx - bw / 2, fileButtonY + 26, bw, 20).build());

        updateButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractMenuBackground(graphics);

        int cx = width / 2;
        int sidebarRight = SIDEBAR_WIDTH;
        int listTop = 22;
        int listBottom = height - 22;
        int listHeight = listBottom - listTop;

        graphics.fill(0, 0, sidebarRight, height, 0x88000000);

        graphics.text(font, Component.literal("\u00a76\u00a7lAutoBuilder"), 6, 6, 0xFFFFFFFF);

        schematics = schematicManager.getAllSchematics();

        int totalItemHeight = schematics.size() * ITEM_HEIGHT;
        int maxScroll = Math.max(0, totalItemHeight - listHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int y = listTop - scrollOffset;
        for (int i = 0; i < schematics.size(); i++) {
            SchematicInfo info = schematics.get(i);
            int itemY = y + i * ITEM_HEIGHT;
            if (itemY + ITEM_HEIGHT < listTop || itemY > listBottom) continue;

            boolean hovered = mouseX >= 2 && mouseX <= sidebarRight - 2
                    && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            boolean selected = i == selectedIndex;

            int bgColor;
            if (selected) {
                bgColor = hovered ? 0xAA444488 : 0xAA333366;
            } else {
                bgColor = hovered ? 0xAA333333 : 0xAA222222;
            }

            if (itemY + ITEM_HEIGHT > listTop) {
                graphics.fill(2, Math.max(itemY, listTop), sidebarRight - 2,
                        Math.min(itemY + ITEM_HEIGHT, listBottom), bgColor);
            }

            graphics.text(font, info.getName(), 6, itemY + 2, 0xFFFFFFFF);
            graphics.text(font,
                    "\u00a77" + info.getWidth() + "\u00d7" + info.getHeight() + "\u00d7" + info.getLength()
                            + "  " + info.getTotalVolume() + " blocks",
                    6, itemY + 12, 0xFFAAAAAA);
        }

        int rightX = sidebarRight + 20;
        int infoY = 22;

        graphics.text(font, Component.literal("\u00a77\u00a7lQuick Build"), cx - 80, infoY, 0xFFFFFF);
        infoY += 26;

        graphics.text(font, "\u00a77Place a schematic with Litematica,", cx - 80, infoY, 0xFFAAAAAA);
        infoY += 12;
        graphics.text(font, "\u00a77then click the button above.", cx - 80, infoY, 0xFFAAAAAA);

        infoY = 95;
        graphics.text(font, Component.literal("\u00a77\u00a7lFile Browser"), cx - 80, infoY, 0xFFFFFF);

        if (selectedSchematic != null) {
            infoY = 22;
            graphics.text(font, Component.literal("\u00a76\u00a7l" + selectedSchematic.getName()),
                    rightX, infoY, 0xFFFFFFFF);

            infoY += 14;
            graphics.text(font, "\u00a77Size: \u00a7f" + selectedSchematic.getWidth()
                            + " \u00d7 " + selectedSchematic.getHeight()
                            + " \u00d7 " + selectedSchematic.getLength(),
                    rightX, infoY, 0xFFAAAAAA);

            infoY += 12;
            graphics.text(font, "\u00a77Volume: \u00a7f" + selectedSchematic.getTotalVolume() + " blocks",
                    rightX, infoY, 0xFFAAAAAA);

            infoY += 12;
            graphics.text(font, "\u00a77File: \u00a7f" + selectedSchematic.getFileName(),
                    rightX, infoY, 0xFFAAAAAA);
        }

        String statusText;
        BuildJob job = buildManager.getCurrentJob();
        if (job != null) {
            BuildState state = job.getState();
            int progress = job.getTotalBlocks() > 0
                    ? (int) (job.getBlocksPlaced() * 100.0 / job.getTotalBlocks()) : 0;

            statusText = "\u00a7eStatus: \u00a7f" + state;
            if (state == BuildState.BUILDING) {
                statusText += " \u00a77(" + job.getBlocksPlaced() + "/" + job.getTotalBlocks() + " blocks, "
                        + progress + "%)";
            } else if (state == BuildState.GATHERING_MATERIALS) {
                long missing = job.getRequirements().stream()
                        .filter(r -> !r.isFulfilled()).count();
                statusText += " \u00a77(" + missing + " material types remaining)";
            }
        } else {
            statusText = "\u00a77Status: Idle";
        }

        graphics.text(font, statusText, sidebarRight + 20, height - 16, 0xFFFFFFFF);

        if (statusMessage != null) {
            long elapsed = System.currentTimeMillis() - statusMessageTime;
            if (elapsed < 5000) {
                int alpha = elapsed > 4000 ? (int) (255 * (5000 - elapsed) / 1000) : 255;
                int color = (alpha << 24) | 0xFFFFFF;
                graphics.text(font, statusMessage, sidebarRight + 20, height - 30, color);
            } else {
                statusMessage = null;
            }
        }

        if (schematics.isEmpty()) {
            graphics.text(font, "\u00a77No .schematic / .schem / .litematic files found.", 6, height / 2 - 4, 0xFFAAAAAA);
            graphics.text(font, "\u00a77Place files in autobuilder/schematics/ and reload.", 6, height / 2 + 8, 0xFF777777);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.buttonInfo().button() == 0 && event.x() >= 2 && event.x() <= SIDEBAR_WIDTH - 2) {
            int listTop = 22;
            for (int i = 0; i < schematics.size(); i++) {
                int itemY = listTop - scrollOffset + i * ITEM_HEIGHT;
                if (event.y() >= itemY && event.y() < itemY + ITEM_HEIGHT) {
                    selectSchematic(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX <= SIDEBAR_WIDTH) {
            scrollOffset -= (int) (vertical * ITEM_HEIGHT);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void selectSchematic(int index) {
        if (index < 0 || index >= schematics.size()) return;
        selectedIndex = index;
        selectedSchematic = schematics.get(index);
        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = selectedSchematic != null;
        boolean isBusy = buildManager.isBusy();

        litematicaButton.active = !isBusy;
        startButton.active = hasSelection && !isBusy;
        gatherButton.active = hasSelection && !isBusy;
        cancelButton.active = isBusy;
        reloadButton.active = !isBusy;
    }

    private void buildLitematica() {
        boolean started = buildManager.startLitematicaBuild();
        if (started) {
            setStatus("\u00a7eBuilding from Litematica placement...");
        } else {
            setStatus("\u00a7cFailed: Litematica mod not found or no placement active.");
        }
        updateButtons();
    }

    private void startBuild() {
        if (selectedSchematic == null) return;
        boolean started = buildManager.startBuild(selectedSchematic.getName());
        if (started) {
            setStatus("\u00a7aStarted building " + selectedSchematic.getName());
        } else {
            setStatus("\u00a7cFailed to start build. Check chat for details.");
        }
        updateButtons();
    }

    private void gatherOnly() {
        if (selectedSchematic == null) return;
        var blockCounts = BlockCounter.countRequiredBlocks(selectedSchematic);
        var reqs = AutoBuilderMod.getInstance().getMaterialManager().analyzeRequirements(blockCounts);
        long missing = reqs.stream().filter(r -> !r.isFulfilled()).count();

        if (missing == 0) {
            setStatus("\u00a7aYou already have all materials!");
            return;
        }

        boolean started = AutoBuilderMod.getInstance().getMaterialManager().startGathering(reqs);
        if (started) {
            setStatus("\u00a7eStarted gathering " + missing + " material types");
        } else {
            setStatus("\u00a7cFailed to start gathering.");
        }
        updateButtons();
    }

    private void cancelBuild() {
        buildManager.cancelBuild();
        setStatus("\u00a7cBuild cancelled");
        updateButtons();
    }

    private void reloadSchematics() {
        schematicManager.reload();
        schematics = schematicManager.getAllSchematics();
        selectedIndex = -1;
        selectedSchematic = null;
        scrollOffset = 0;
        setStatus("\u00a7aReloaded schematics. Found \u00a76" + schematics.size() + "\u00a7a files.");
        updateButtons();
    }

    private void setStatus(String msg) {
        this.statusMessage = msg;
        this.statusMessageTime = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
