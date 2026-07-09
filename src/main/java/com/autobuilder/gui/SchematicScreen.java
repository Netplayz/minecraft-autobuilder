package com.autobuilder.gui;

import com.autobuilder.AutoBuilderMod;
import com.autobuilder.builder.BuildJob;
import com.autobuilder.builder.BuildManager;
import com.autobuilder.builder.BuildState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class SchematicScreen extends Screen {
    private final BuildManager buildManager;

    private String statusMessage;
    private long statusMessageTime;

    private Button litematicaButton;
    private Button cancelButton;

    private BuildJob lastTrackedJob;
    private BuildState lastTrackedState;

    private record LogEntry(long time, String text) {}
    private final List<LogEntry> eventLog = new ArrayList<>();
    private int logScroll;

    public SchematicScreen() {
        super(Component.literal("AutoBuilder"));
        this.buildManager = AutoBuilderMod.getInstance().getBuildManager();
        log("§7AutoBuilder ready");
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int bw = 200;

        litematicaButton = addRenderableWidget(Button.builder(
                Component.literal("\u00a76\u00a7lBuild Litematica Placement"),
                btn -> buildLitematica()
        ).bounds(cx - bw / 2, height / 2 - 30, bw, 24).build());

        cancelButton = addRenderableWidget(Button.builder(
                Component.literal("Cancel Build"),
                btn -> cancelBuild()
        ).bounds(cx - bw / 2, height / 2 + 6, bw, 20).build());

        updateButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractMenuBackground(graphics);

        int cx = width / 2;

        graphics.text(font, Component.literal("\u00a76\u00a7lAutoBuilder"), cx - 40, height / 2 - 70, 0xFFFFFFFF);

        graphics.text(font, "\u00a77Place a schematic with Litematica,", cx - 80, height / 2 - 52, 0xFFAAAAAA);
        graphics.text(font, "\u00a77then click Build.", cx - 80, height / 2 - 40, 0xFFAAAAAA);

        String statusText;
        BuildJob job = buildManager.getCurrentJob();
        if (job != null) {
            BuildState state = job.getState();
            int total = job.getTotalBlocks();

            statusText = "\u00a7eStatus: \u00a7f" + state;
            if (state == BuildState.BUILDING) {
                if (total > 0) {
                    int progress = (int) (job.getBlocksPlaced() * 100.0 / total);
                    statusText += " \u00a77(" + job.getBlocksPlaced() + "/" + total + " blocks, "
                            + progress + "%)";
                } else {
                    statusText += " \u00a77(progress unknown for Litematica builds)";
                }
            } else if (state == BuildState.GATHERING_MATERIALS) {
                long missing = job.getRequirements().stream()
                        .filter(r -> !r.isFulfilled()).count();
                statusText += " \u00a77(" + missing + " material types remaining)";
            }

            if (job != lastTrackedJob || state != lastTrackedState) {
                if (lastTrackedJob == null && job != null) {
                    log("\u00a7eBuild started: \u00a7f" + job.getSchematic().getName());
                } else if (state == BuildState.COMPLETED) {
                    log("\u00a7aBuild complete: \u00a7f" + job.getSchematic().getName());
                } else if (state == BuildState.FAILED) {
                    log("\u00a7cBuild failed: \u00a7f" + (job.getErrorMessage() != null ? job.getErrorMessage() : "unknown error"));
                } else if (state == BuildState.IDLE) {
                    log("\u00a77Build idle");
                } else if (state == BuildState.GATHERING_MATERIALS) {
                    log("\u00a7eGathering materials...");
                } else if (state == BuildState.BUILDING && lastTrackedState == BuildState.GATHERING_MATERIALS) {
                    log("\u00a7aStarting construction");
                }
                lastTrackedJob = job;
                lastTrackedState = state;
            }
        } else {
            statusText = "\u00a77Status: Idle";
            if (lastTrackedJob != null) {
                log("\u00a77Job cleared");
                lastTrackedJob = null;
                lastTrackedState = null;
            }
        }

        renderEventLog(graphics);

        graphics.text(font, statusText, cx - 80, height - 30, 0xFFFFFFFF);

        if (statusMessage != null) {
            long elapsed = System.currentTimeMillis() - statusMessageTime;
            if (elapsed < 5000) {
                int alpha = elapsed > 4000 ? (int) (255 * (5000 - elapsed) / 1000) : 255;
                int color = (alpha << 24) | 0xFFFFFF;
                graphics.text(font, statusMessage, cx - 80, height - 44, color);
            } else {
                statusMessage = null;
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void renderEventLog(GuiGraphicsExtractor graphics) {
        int logLeft = 10;
        int logTop = height / 2 + 40;
        int logRight = width - 10;
        int logBottom = height - 56;
        if (logTop >= logBottom) return;

        int logHeight = logBottom - logTop;

        graphics.fill(logLeft, logTop, logRight, logBottom, 0x88000000);
        graphics.text(font, Component.literal("\u00a78\u00a7lEvent Log"), logLeft + 4, logTop + 4, 0xFF888888);

        int availableHeight = logHeight - 14;
        int lineHeight = 10;
        int visibleLines = availableHeight / lineHeight;
        if (visibleLines <= 0) return;

        int totalLines = eventLog.size();
        int maxScroll = Math.max(0, totalLines - visibleLines);
        if (logScroll > maxScroll) logScroll = maxScroll;
        if (logScroll < 0) logScroll = 0;

        int startIdx = totalLines - visibleLines - logScroll;
        if (startIdx < 0) startIdx = 0;

        for (int i = 0; i < visibleLines && startIdx + i < totalLines; i++) {
            LogEntry entry = eventLog.get(startIdx + i);
            long age = System.currentTimeMillis() - entry.time;
            int alpha = age < 10000 ? 0xFF : (int) Math.max(80, 0xFF - (age - 10000) * 8);
            int color = (alpha << 24) | 0xAAAAAA;
            graphics.text(font, entry.text, logLeft + 4, logTop + 14 + i * lineHeight, color);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseY > height / 2 + 40 && mouseY < height - 56) {
            logScroll -= (int) vertical;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    private void updateButtons() {
        boolean isBusy = buildManager.isBusy();
        litematicaButton.active = !isBusy;
        cancelButton.active = isBusy;
    }

    private void buildLitematica() {
        boolean started = buildManager.startLitematicaBuild();
        if (started) {
            log("\u00a7eBuilding from Litematica placement...");
            setStatus("\u00a7eBuilding from Litematica placement...");
        } else {
            log("\u00a7cFailed: Litematica mod not found or no placement active.");
            setStatus("\u00a7cFailed: Litematica mod not found or no placement active.");
        }
        updateButtons();
    }

    private void cancelBuild() {
        if (buildManager.getCurrentJob() != null) {
            log("\u00a7cBuild cancelled: \u00a7f" + buildManager.getCurrentJob().getSchematic().getName());
        }
        buildManager.cancelBuild();
        setStatus("\u00a7cBuild cancelled");
        updateButtons();
    }

    private void log(String msg) {
        eventLog.add(new LogEntry(System.currentTimeMillis(), msg));
        if (eventLog.size() > 200) {
            eventLog.remove(0);
        }
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
