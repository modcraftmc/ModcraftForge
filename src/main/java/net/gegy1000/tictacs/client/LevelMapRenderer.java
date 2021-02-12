package net.gegy1000.tictacs.client;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.gegy1000.tictacs.chunk.ChunkAccess;
import net.gegy1000.tictacs.chunk.ChunkController;
import net.gegy1000.tictacs.chunk.ChunkLevelTracker;
import net.gegy1000.tictacs.chunk.entry.ChunkEntry;
import net.gegy1000.tictacs.chunk.step.ChunkStep;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;

import java.util.Collection;

public final class LevelMapRenderer {
    private static final Object2IntMap<ChunkStep> STATUS_TO_COLOR = Util.make(new Object2IntOpenHashMap<>(), map -> {
        map.defaultReturnValue(0);
        map.put(ChunkStep.EMPTY, 0xFF545454);
        map.put(ChunkStep.STRUCTURE_STARTS, 0xFF999999);
        map.put(ChunkStep.SURFACE, 0xFF723530);
        map.put(ChunkStep.FEATURES, 0xFF00C621);
        map.put(ChunkStep.LIGHTING, 0xFFA0A0A0);
        map.put(ChunkStep.FULL, 0xFFFFFFFF);
    });

    private static final int TICKABLE = 0xFF0000FF;
    private static final int ENTITY_TICKABLE = 0xFF00FFFF;

    private static final int MAX_LEVEL = ChunkLevelTracker.MAX_LEVEL + 1;

    private static final RenderType TYPE = RenderType.STEP;

    public static NativeImage render(Vector3d camera, ChunkController controller) {
        ChunkAccess map = controller.getMap().visible();
        Collection<ChunkEntry> entries = map.getEntries();

        Rectangle2d bounds = computeBounds(entries);

        NativeImage image;
        switch (TYPE) {
            case STEP:
                image = renderSteps(map, bounds);
                break;
            case LEVEL:
                image = renderLeveled(controller, map, bounds);
                break;
            default: throw new UnsupportedOperationException();
        }

        int cameraX = MathHelper.floor(camera.x / 16.0) - bounds.getX();
        int cameraZ = MathHelper.floor(camera.z / 16.0) - bounds.getY();

        if (cameraX >= 0 && cameraZ >= 0 && cameraX < bounds.getWidth() && cameraZ < bounds.getHeight()) {
            image.setPixelRGBA(cameraX, cameraZ, 0xFF0000FF);
        }

        return image;
    }

    private static NativeImage renderSteps(ChunkAccess map, Rectangle2d bounds) {
        int minX = bounds.getX();
        int minY = bounds.getY();

        int width = bounds.getWidth();
        int height = bounds.getHeight();

        NativeImage image = new NativeImage(width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                long pos = ChunkPos.asLong(x + minX, y + minY);
                ChunkEntry entry = map.getEntry(pos);

                int color = 0;
                if (entry != null && ChunkLevelTracker.isLoaded(entry.getChunkLevel())) {
                    color = getColorForChunk(entry);
                }

                image.setPixelRGBA(x, y, color);
            }
        }

        return image;
    }

    private static int getColorForChunk(ChunkEntry entry) {
        ChunkStep currentStep = entry.getCurrentStep();
        if (currentStep != null) {
            if (entry.isTickingEntities()) {
                return ENTITY_TICKABLE;
            }

            if (entry.isTicking()) {
                return TICKABLE;
            }

            return STATUS_TO_COLOR.getInt(currentStep);
        }

        return 0;
    }

    private static NativeImage renderLeveled(ChunkController controller, ChunkAccess map, Rectangle2d bounds) {
        TicketManager ticketManager = controller.getTicketManager();

        int minX = bounds.getX();
        int minY = bounds.getY();

        int width = bounds.getWidth();
        int height = bounds.getHeight();

        NativeImage image = new NativeImage(width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                long pos = ChunkPos.asLong(x + minX, y + minY);
                ChunkEntry entry = map.getEntry(pos);

                SortedArraySet<Ticket<?>> tickets = ticketManager.tickets.get(pos);
                if (tickets != null && !tickets.isEmpty()) {
                    boolean player = false;
                    boolean light = false;
                    for (Ticket<?> ticket : tickets) {
                        TicketType<?> type = ticket.getType();
                        if (type == TicketType.PLAYER) player = true;
                        if (type == TicketType.LIGHT) light = true;
                    }

                    int color = 0xFF0000FF;
                    if (player) color |= 0xFF0000; // blue
                    if (light) color |= 0xFF00; // green

                    image.setPixelRGBA(x, y, color);

                    continue;
                }

                if (entry != null && ChunkLevelTracker.isLoaded(entry.getChunkLevel())) {
                    int level = entry.getChunkLevel();
                    int levelRange = MAX_LEVEL - ChunkLevelTracker.FULL_LEVEL;

                    int renderLevel = Math.max(level, ChunkLevelTracker.FULL_LEVEL) - ChunkLevelTracker.FULL_LEVEL;

                    int brightness = (levelRange - renderLevel) * 255 / levelRange;
                    int color = (0xFF << 24) | (brightness << 16) | (brightness << 8) | brightness;

                    image.setPixelRGBA(x, y, color);
                } else {
                    image.setPixelRGBA(x, y, 0xFFFF0000);
                }
            }
        }

        return image;
    }

    private static Rectangle2d computeBounds(Collection<ChunkEntry> entries) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (ChunkEntry entry : entries) {
            ChunkPos pos = entry.getPosition();
            int x = pos.x;
            int z = pos.z;

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;

            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }

        return new Rectangle2d(minX, minZ, maxX - minX, maxZ - minZ);
    }

    private enum RenderType {
        LEVEL,
        STEP
    }
}
