package CreateSableBridge.bridge.Bearing;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber
public class MechanicalBearingBlockEntitySubLevelBridge {

    private static final Map<BlockPos, RotaryConstraintHandle> constraintHandles = new ConcurrentHashMap<>();

    public static UUID createBearingSubLevel(ServerLevel level, BlockPos bearingPos, Direction facing,
                                             Map<BlockPos, BlockState> blocks, float speedRpm) {
        if (blocks == null || blocks.isEmpty())
            throw new IllegalArgumentException("Cannot assemble an empty block map");

        Map<BlockPos, BlockState> valid = new HashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : blocks.entrySet()) {
            int y = e.getKey().getY();
            if (y >= level.getMinBuildHeight() && y <= level.getMaxBuildHeight())
                valid.put(e.getKey(), e.getValue());
        }
        if (valid.isEmpty())
            throw new IllegalArgumentException("No valid blocks remaining after Y range filtering!");

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos p : valid.keySet()) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        BoundingBox3ic bounds = new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);

        ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, bearingPos, valid.keySet(), bounds);
        if (subLevel == null || subLevel.getPlot() == null || subLevel.getPlot().getBoundingBox() == null)
            throw new IllegalStateException("Failed to assemble Sable SubLevel or bounds are null");

        UUID id = subLevel.getUniqueId();
        BearingSavedData saved = BearingSavedData.get(level);
        saved.bearings.put(bearingPos, new BearingSavedData.BearingEntry(id, facing));
        saved.setDirty();

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container != null) {
            container.addForceLoadTicket(subLevel, SubLevelLoadingTicketType.COMMAND_FORCED, Unit.INSTANCE);
            applyRotaryConstraint(container, subLevel, bearingPos, facing);
        }
        return id;
    }

    private static void applyRotaryConstraint(ServerSubLevelContainer container, ServerSubLevel subLevel,
                                              BlockPos bearingPos, Direction facing) {
        Vector3d worldPivot = new Vector3d(
                bearingPos.getX() + 0.5 + facing.getStepX() * 0.5,
                bearingPos.getY() + 0.5 + facing.getStepY() * 0.5,
                bearingPos.getZ() + 0.5 + facing.getStepZ() * 0.5);

        Direction.Axis a = facing.getAxis();
        Vector3d worldAxis = new Vector3d(
                a == Direction.Axis.X ? 1 : 0,
                a == Direction.Axis.Y ? 1 : 0,
                a == Direction.Axis.Z ? 1 : 0);

        Vector3d localPivot = subLevel.logicalPose().transformPositionInverse(new Vector3d(worldPivot));
        Vector3d localAxis = subLevel.logicalPose().transformNormalInverse(new Vector3d(worldAxis)).normalize();

        var handle = container.physicsSystem().getPipeline().addConstraint(null, subLevel,
                new RotaryConstraintConfiguration(worldPivot, localPivot, worldAxis, localAxis));
        if (handle instanceof RotaryConstraintHandle rotary)
            constraintHandles.put(bearingPos.immutable(), rotary);
    }

    private static void spinAbout(ServerSubLevel subLevel, Direction facing, float speedRpm) {
        RigidBodyHandle body = RigidBodyHandle.of(subLevel);
        if (body == null || !body.isValid()) return;

        Direction.Axis a = facing.getAxis();
        Vector3d axis = new Vector3d(
                a == Direction.Axis.X ? 1 : 0,
                a == Direction.Axis.Y ? 1 : 0,
                a == Direction.Axis.Z ? 1 : 0);

        double target = speedRpm * Math.PI / 30.0;
        double delta = target - body.getAngularVelocity(new Vector3d()).dot(axis);
        if (Math.abs(delta) < 1e-4) return;
        body.addLinearAndAngularVelocity(new Vector3d(), axis.mul(delta));
    }

    /** Разборку делает Create: здесь только записываем реальный угол и убираем сабуровень. */
    public static void releaseForCreate(ServerLevel level, BlockPos bearingPos, AbstractContraptionEntity entity) {
        BearingSavedData saved = BearingSavedData.get(level);
        BearingSavedData.BearingEntry e = saved.bearings.remove(bearingPos);
        saved.setDirty();

        RotaryConstraintHandle h = constraintHandles.remove(bearingPos);
        if (h != null && h.isValid()) h.remove();
        if (e == null) return;

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;
        ServerSubLevel sl = (ServerSubLevel) container.getSubLevel(e.subLevelId());
        if (sl == null || sl.isRemoved()) return;

        if (entity instanceof ControlledContraptionEntity cce)
            cce.setAngle((float) Math.toDegrees(twistAngle(sl, e.axis().getAxis())));
        sl.markRemoved();
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

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;

        for (Map.Entry<BlockPos, BearingSavedData.BearingEntry> en : BearingSavedData.get(level).bearings.entrySet()) {
            ServerSubLevel sl = (ServerSubLevel) container.getSubLevel(en.getValue().subLevelId());
            if (sl != null && !sl.isRemoved())
                applyRotaryConstraint(container, sl, en.getKey(), en.getValue().axis());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getLevel().dimension().location().getNamespace().equals("sable")) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerSubLevelContainer container = (ServerSubLevelContainer) SubLevelContainer.getContainer(level);
        if (container == null) return;

        for (Map.Entry<BlockPos, BearingSavedData.BearingEntry> en : BearingSavedData.get(level).bearings.entrySet()) {
            ServerSubLevel sl = (ServerSubLevel) container.getSubLevel(en.getValue().subLevelId());
            if (sl == null || sl.isRemoved()) continue;
            float rpm = level.getBlockEntity(en.getKey()) instanceof KineticBlockEntity k ? k.getSpeed() : 0f;
            spinAbout(sl, en.getValue().axis(), rpm);
        }
    }
}