package CreateSableBridge.bridge.Bearing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BearingSavedData extends SavedData {
    private static final String DATA_NAME = "createsablebridge_bearings";

    public final Map<BlockPos, BearingEntry> bearings = new HashMap<>();

    public record BearingEntry(UUID subLevelId, Direction axis) {}

    public static BearingSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(BearingSavedData::new, BearingSavedData::load, null),
                DATA_NAME
        );
    }

    public static BearingSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        BearingSavedData data = new BearingSavedData();
        ListTag list = tag.getList("Bearings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            UUID uuid = entry.getUUID("uuid");
            Direction axis = Direction.from3DDataValue(entry.getInt("axis"));
            data.bearings.put(pos, new BearingEntry(uuid, axis));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, BearingEntry> entry : bearings.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", entry.getKey().asLong());
            c.putUUID("uuid", entry.getValue().subLevelId());
            c.putInt("axis", entry.getValue().axis().get3DDataValue());
            list.add(c);
        }
        tag.put("Bearings", list);
        return tag;
    }
}