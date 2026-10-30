package CreateSableBridge;

import CreateSableBridge.Interfaces.SableContraptionHandler;
import com.simibubi.create.api.contraption.ContraptionType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SableContraptionHandlers {
    private static final Map<ContraptionType, SableContraptionHandler> MAP = new ConcurrentHashMap<>();
    public static void register(ContraptionType t, SableContraptionHandler h) { MAP.put(t, h); }
    public static SableContraptionHandler get(ContraptionType t) { return MAP.get(t); }
}