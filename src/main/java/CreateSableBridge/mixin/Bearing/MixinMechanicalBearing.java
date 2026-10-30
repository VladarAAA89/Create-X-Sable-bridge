//package CreateSableBridge.mixin.Bearing;
//
//import CreateSableBridge.bridge.Bearing.BearingSavedData;
//import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
//import net.minecraft.server.level.ServerLevel;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
//public abstract class MixinMechanicalBearing {
//
//    @Shadow protected boolean running;
//    @Shadow protected float angle;
//
//    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
//    private void createsable$assemble(CallbackInfo ci) {
//        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;
//        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
//        ServerLevel level = (ServerLevel) be.getLevel();
//
//        // Если BearingHandler уже успешно собрал саб-уровень через Sable
//        BearingSavedData savedData = BearingSavedData.get(level);
//        if (savedData.bearings.containsKey(be.getBlockPos())) {
//            this.running = true;
//            this.angle = 0;
//            be.sendData();
//            be.updateGeneratedRotation();
//
//            // Отменяем стандартный метод Create, чтобы он не спавнил свой ControlledContraptionEntity
//            ci.cancel();
//        }
//    }
//}