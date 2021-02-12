package net.gegy1000.tictacs;

import net.gegy1000.tictacs.client.TicTacsDebugLevelTracker;

public class TicTacsClient  {
    public void onInitializeClient() {
        /*
        ClientSidePacketRegistry.INSTANCE.register(TicTacs.DEBUG_CHUNK_TICKETS, (packetContext, data) -> {
            long chunkPos = data.readLong();
            int toLevel = data.readInt();

            packetContext.getTaskQueue().execute(() -> {
                TicTacsDebugLevelTracker.INSTANCE.setLevel(chunkPos, toLevel);
            });
        });

         */
    }
}
