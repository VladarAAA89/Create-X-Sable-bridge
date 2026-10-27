package CreateSableBridge.bridge.Bearing;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class MechanicalBearingBlockEntitySubLevelBridge {

    private static final Map<BlockPos, UUID> activeSubLevels = new ConcurrentHashMap<>();

    private static final Map<BlockPos, Double> targetAngles = new ConcurrentHashMap<>();
    private static final double STIFFNESS = 200.0;  // стартовое значение, подбирать
    private static final double DAMPING = 30.0;     // ≈ 2·√STIFFNESS

    public static UUID createBearingSubLevel(
            ServerLevel level,
            BlockPos bearingPos,
            Direction rotationAxis,
            Map<BlockPos, BlockState> blocks,
            float speedRpm,
            List<AABB> glueBoxes
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
        savedData.bearings.put(bearingPos, new BearingSavedData.BearingEntry(subLevelId, rotationAxis, glueBoxes));
        savedData.setDirty();

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container != null) {
            container.addForceLoadTicket(subLevel, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
            applyRotaryConstraint(container, subLevel, bearingPos, rotationAxis);
        }

        return subLevelId;
    }

    // поле класса. Object замени на тип, который вернёт addConstraint
// (наведи курсор на handle ниже): он нужен для setMotor и remove
    private static final Map<BlockPos, RotaryConstraintHandle> constraintHandles = new ConcurrentHashMap<>();

    private static void applyRotaryConstraint(ServerSubLevelContainer container, ServerSubLevel subLevel,
                                              BlockPos bearingPos, Direction facing) {
        // 1. Пивот: центр грани подшипника, к которой крепится платформа
        Vector3d worldPivot = new Vector3d(
                bearingPos.getX() + 0.5 + facing.getStepX() * 0.5,
                bearingPos.getY() + 0.5 + facing.getStepY() * 0.5,
                bearingPos.getZ() + 0.5 + facing.getStepZ() * 0.5);

        // 2. Ось вращения: всегда положительное направление
        Direction.Axis a = facing.getAxis();
        Vector3d worldAxis = new Vector3d(
                a == Direction.Axis.X ? 1 : 0,
                a == Direction.Axis.Y ? 1 : 0,
                a == Direction.Axis.Z ? 1 : 0);

        // 3. Перевод в локальные координаты сабуровня
        Vector3d localPivot = subLevel.logicalPose().transformPositionInverse(new Vector3d(worldPivot));
        Vector3d localAxis = subLevel.logicalPose().transformNormalInverse(new Vector3d(worldAxis)).normalize();

        RotaryConstraintConfiguration config = new RotaryConstraintConfiguration(
                worldPivot, localPivot, worldAxis, localAxis);

        var handle = container.physicsSystem().getPipeline().addConstraint(null, subLevel, config);
        if (handle instanceof RotaryConstraintHandle rotary) {
            constraintHandles.put(bearingPos.immutable(), rotary);
        }
    }

    private static void spinAbout(ServerSubLevel subLevel, Direction facing, float speedRpm) {
        RigidBodyHandle body = RigidBodyHandle.of(subLevel);
        if (body == null || !body.isValid()) return;

        Direction.Axis a = facing.getAxis();
        Vector3d axis = new Vector3d(
                a == Direction.Axis.X ? 1 : 0,
                a == Direction.Axis.Y ? 1 : 0,
                a == Direction.Axis.Z ? 1 : 0);

        double target = speedRpm * Math.PI / 30.0;   // рад/с

        Vector3d current = body.getAngularVelocity(new Vector3d());
        double delta = target - current.dot(axis);
        if (Math.abs(delta) < 1e-4) return;

        body.addLinearAndAngularVelocity(new Vector3d(), axis.mul(delta));
    }

    public static void removeBearingSubLevel(ServerLevel level, BlockPos bearingPos) {
        BearingSavedData savedData = BearingSavedData.get(level);
        if (savedData == null) return;

        BlockPos key = bearingPos;
        BearingSavedData.BearingEntry entry = savedData.bearings.remove(key);
        if (entry == null) {
            for (BlockPos storedPos : savedData.bearings.keySet()) {
                if (storedPos.closerThan(bearingPos, 1.5)) { key = storedPos; break; }
            }
            entry = savedData.bearings.remove(key);
        }
        savedData.setDirty();
        if (entry == null) return;

        RotaryConstraintHandle h = constraintHandles.remove(key);
        if (h != null && h.isValid()) h.remove();
        activeSubLevels.remove(key);

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;
        ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(entry.subLevelId());
        if (subLevel == null || subLevel.isRemoved()) return;

        Direction facing = entry.axis();
        Direction.Axis axis = facing.getAxis();
        BlockPos anchorPos = key.relative(facing);

        // реальный угол и ближайший кратный 90°
        double theta = twistAngle(subLevel, axis);
        float snapDeg = Math.round(Math.toDegrees(theta) / 90.0) * 90f;
        StructureTransform transform = new StructureTransform(anchorPos,
                axis == Direction.Axis.X ? snapDeg : 0f,
                axis == Direction.Axis.Y ? snapDeg : 0f,
                axis == Direction.Axis.Z ? snapDeg : 0f);

        Map<BlockPos, BlockState> result = collectSnapped(subLevel, anchorPos, axis, theta, transform);

        subLevel.markRemoved();

        // сначала обычные блоки, потом хрупкие (факелы, лестницы), чтобы не отваливались
        for (boolean brittle : new boolean[]{false, true}) {
            for (Map.Entry<BlockPos, BlockState> e : result.entrySet()) {
                if (BlockMovementChecks.isBrittle(e.getValue()) == brittle)
                    level.setBlock(e.getKey(), e.getValue(), 3);
            }
        }

        // клей возвращаем уже повёрнутым
        for (AABB box : entry.glue()) {
            Vec3 c1 = transform.apply(new Vec3(box.minX, box.minY, box.minZ));
            Vec3 c2 = transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ));
            level.addFreshEntity(new SuperGlueEntity(level, new AABB(c1, c2)));
        }
    }

    /** Поворот вокруг оси (swing-twist), радианы, диапазон (-π, π]. */
    private static double twistAngle(ServerSubLevel subLevel, Direction.Axis axis) {
        var q = subLevel.logicalPose().orientation();
        double s = axis == Direction.Axis.X ? q.x() : axis == Direction.Axis.Y ? q.y() : q.z();
        double t = 2.0 * Math.atan2(s, q.w());
        if (t > Math.PI) t -= 2 * Math.PI;
        if (t < -Math.PI) t += 2 * Math.PI;
        return t;
    }

    private static Map<BlockPos, BlockState> collectSnapped(ServerSubLevel subLevel, BlockPos anchorPos,
                                                            Direction.Axis axis, double theta, StructureTransform transform) {
        Map<BlockPos, BlockState> out = new HashMap<>();
        ServerLevel subWorld = subLevel.getLevel();
        if (subWorld == null || subLevel.getPlot() == null) return out;
        BoundingBox3ic b = subLevel.getPlot().getBoundingBox();
        if (b == null) return out;

        Vector3d pivot = new Vector3d(anchorPos.getX() + 0.5, anchorPos.getY() + 0.5, anchorPos.getZ() + 0.5);
        double ax = axis == Direction.Axis.X ? 1 : 0;
        double ay = axis == Direction.Axis.Y ? 1 : 0;
        double az = axis == Direction.Axis.Z ? 1 : 0;

        for (int x = b.minX(); x <= b.maxX(); x++)
            for (int y = b.minY(); y <= b.maxY(); y++)
                for (int z = b.minZ(); z <= b.maxZ(); z++) {
                    BlockState state = subWorld.getBlockState(new BlockPos(x, y, z));
                    if (state == null || state.isAir()) continue;

                    Vector3d p = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                    subLevel.logicalPose().transformPosition(p);                 // где блок сейчас
                    p.sub(pivot).rotateAxis(-theta, ax, ay, az).add(pivot);      // где был при угле 0

                    BlockPos original = BlockPos.containing(p.x, p.y, p.z);
                    BlockPos local = original.subtract(anchorPos);
                    out.put(transform.apply(local), transform.apply(state));
                }
        return out;
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

//    public static void updateVelocityByPos(ServerLevel level, BlockPos bearingPos, float speedRpm) {
//        BearingSavedData savedData = BearingSavedData.get(level);
//        if (savedData == null) return;
//
//        BearingSavedData.BearingEntry entry = savedData.bearings.get(bearingPos);
//        if (entry == null) return;
//
//        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
//        if (container == null) return;
//
//        ServerSubLevel subLevel = (ServerSubLevel) container.getSubLevel(entry.subLevelId());
//        if (subLevel != null && !subLevel.isRemoved()) {
//            driveBearing(subLevel, entry.axis(), speedRpm);
//        }
//    }

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
            ServerSubLevel sl = (ServerSubLevel) container.getSubLevel(entry.getValue().subLevelId());
            if (sl == null || sl.isRemoved()) continue;
            spinAbout(sl, entry.getValue().axis(), getSpeedFromBearing(level, entry.getKey()));
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