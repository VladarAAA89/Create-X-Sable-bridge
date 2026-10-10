package CreateSableBridge.bridge.Bearing;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class MechanicalBearingBlockEntitySubLevelBridge {

    private static final Map<BlockPos, UUID> activeSubLevels = new ConcurrentHashMap<>();

    public static UUID createBearingSubLevel(
            ServerLevel level,
            BlockPos bearingPos,
            Direction rotationAxis,
            Map<BlockPos, BlockState> blocks,
            float speedRpm
    ) {
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalArgumentException("Cannot assemble an empty block map");
        }

        Map<BlockPos, BlockState> validBlocks = new HashMap<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() >= level.getMinBuildHeight() && pos.getY() <= level.getMaxBuildHeight()) {
                validBlocks.put(pos, entry.getValue());
            }
        }

        if (validBlocks.isEmpty()) {
            throw new IllegalArgumentException("No valid blocks remaining after Y range filtering!");
        }

        // Вычисляем корректные границы в мировых координатах
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : validBlocks.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        BoundingBox3ic bounds = new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);

        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(
                level,
                bearingPos,
                validBlocks.keySet(),
                bounds
        );

        if (subLevel == null || subLevel.getPlot() == null || subLevel.getPlot().getBoundingBox() == null) {
            throw new IllegalStateException("Failed to assemble Sable SubLevel or bounds are null");
        }

        UUID subLevelId = subLevel.getUniqueId();
        activeSubLevels.put(bearingPos, subLevelId);

        BearingSavedData savedData = BearingSavedData.get(level);
        savedData.bearings.put(bearingPos, new BearingSavedData.BearingEntry(subLevelId, rotationAxis));
        savedData.setDirty();

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container != null) {
            container.addForceLoadTicket(subLevel, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
            applyRotaryConstraint(container, subLevel, bearingPos, rotationAxis);
        }

        updateVelocity(subLevel, rotationAxis, speedRpm);
        return subLevelId;
    }

    private static void applyRotaryConstraint(ServerSubLevelContainer container, ServerSubLevel subLevel, BlockPos bearingPos, Direction rotationAxis) {
        Vector3d worldPivot = new Vector3d(bearingPos.getX() + 0.5, bearingPos.getY() + 0.5, bearingPos.getZ() + 0.5);
        Vector3d localPivot = subLevel.logicalPose().transformPositionInverse(new Vector3d(worldPivot));

        Vector3d worldAxis = new Vector3d(
                rotationAxis.getStepX(),
                rotationAxis.getStepY(),
                rotationAxis.getStepZ()
        ).normalize();

        // Перевод вектора направления в локальные координаты
        Vector3d zeroInv = subLevel.logicalPose().transformPositionInverse(new Vector3d(0, 0, 0));
        Vector3d localAxis = subLevel.logicalPose().transformPositionInverse(new Vector3d(worldAxis)).sub(zeroInv).normalize();

        RotaryConstraintConfiguration constraintConfig = new RotaryConstraintConfiguration(
                worldPivot,
                localPivot,
                worldAxis,
                localAxis
        );

        container.physicsSystem().getPipeline().addConstraint(null, subLevel, constraintConfig);
    }

    public static void updateVelocity(ServerSubLevel subLevel, Direction rotationAxis, float speedRpm) {
        if (subLevel == null || subLevel.isRemoved()) return;

        Vector3d axisNormal = new Vector3d(
                rotationAxis.getStepX(),
                rotationAxis.getStepY(),
                rotationAxis.getStepZ()
        ).normalize();

        double radPerSec = (speedRpm * Math.PI) / 30.0;
        subLevel.latestAngularVelocity.set(axisNormal).mul(radPerSec);
    }

    public static void removeBearingSubLevel(ServerLevel level, BlockPos bearingPos) {
        BearingSavedData savedData = BearingSavedData.get(level);
        if (savedData == null) return;

        BearingSavedData.BearingEntry entry = savedData.bearings.remove(bearingPos);

        if (entry == null) {
            BlockPos foundKey = null;
            for (BlockPos storedPos : savedData.bearings.keySet()) {
                if (storedPos.closerThan(bearingPos, 1.5)) {
                    foundKey = storedPos;
                    break;
                }
            }
            if (foundKey != null) {
                entry = savedData.bearings.remove(foundKey);
            }
        }

        savedData.setDirty();
        if (entry == null) return;

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;

        ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(entry.subLevelId());

        // --- ВОТ ЗДЕСЬ ИСПОЛЬЗУЕТСЯ МАРКИРОВКА ---
        if (subLevel != null && !subLevel.isRemoved()) {
            Map<BlockPos, BlockState> currentSubLevelBlocks = extractBlocksFromSubLevel(subLevel);

            // 1. Помечаем саб-уровень как удаленный
            subLevel.markRemoved();

            // 2. Возвращаем блоки в физический мир
            for (Map.Entry<BlockPos, BlockState> blockEntry : currentSubLevelBlocks.entrySet()) {
                level.setBlock(blockEntry.getKey(), blockEntry.getValue(), 3);
            }
        }
    }

    private static Map<BlockPos, BlockState> extractBlocksFromSubLevel(ServerSubLevel subLevel) {
        Map<BlockPos, BlockState> extractedBlocks = new HashMap<>();
        ServerLevel subWorld = subLevel.getLevel();
        if (subWorld == null || subLevel.getPlot() == null) return extractedBlocks;

        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        if (bounds == null) return extractedBlocks;

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    BlockPos plotPos = new BlockPos(x, y, z);
                    BlockState state = subWorld.getBlockState(plotPos);

                    if (state != null && !state.isAir()) {
                        Vector3d worldVec = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                        subLevel.logicalPose().transformPosition(worldVec);

                        BlockPos worldPos = new BlockPos(
                                (int) Math.floor(worldVec.x),
                                (int) Math.floor(worldVec.y),
                                (int) Math.floor(worldVec.z)
                        );
                        extractedBlocks.put(worldPos, state);
                    }
                }
            }
        }
        return extractedBlocks;
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;

        BearingSavedData savedData = BearingSavedData.get(level);
        for (Map.Entry<BlockPos, BearingSavedData.BearingEntry> entry : savedData.bearings.entrySet()) {
            ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(entry.getValue().subLevelId());
            if (subLevel != null && !subLevel.isRemoved()) {
                activeSubLevels.put(entry.getKey(), entry.getValue().subLevelId());
                applyRotaryConstraint(container, subLevel, entry.getKey(), entry.getValue().axis());
            }
        }
    }

    public static void updateVelocityByPos(ServerLevel level, BlockPos bearingPos, float speedRpm) {
        BearingSavedData savedData = BearingSavedData.get(level);
        if (savedData == null) return;

        BearingSavedData.BearingEntry entry = savedData.bearings.get(bearingPos);
        if (entry == null) return;

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;

        ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(entry.subLevelId());
        if (subLevel != null && !subLevel.isRemoved()) {
            updateVelocity(subLevel, entry.axis(), speedRpm);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos brokenPos = event.getPos();

        BearingSavedData savedData = BearingSavedData.get(level);
        if (savedData != null && savedData.bearings.containsKey(brokenPos)) {
            removeBearingSubLevel(level, brokenPos);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;

        // Проверка namespace измерения (саб-уровни Sable находятся в своем измерении)
        if (event.getLevel().dimension().location().getNamespace().equals("sable")) return;

        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;

        BearingSavedData savedData = BearingSavedData.get(level);
        if (savedData == null) return;

        for (Map.Entry<BlockPos, BearingSavedData.BearingEntry> entry : savedData.bearings.entrySet()) {
            ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(entry.getValue().subLevelId());
            if (subLevel != null && !subLevel.isRemoved()) {
                float currentSpeed = getSpeedFromBearing(level, entry.getKey());
                updateVelocity(subLevel, entry.getValue().axis(), currentSpeed);
            }
        }
    }

    private static float getSpeedFromBearing(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof com.simibubi.create.content.kinetics.base.KineticBlockEntity kineticBE) {
            return kineticBE.getSpeed();
        }
        return 0f;
    }
}