package CreateSableBridge.mixin;

import CreateSableBridge.SableAssemblyHandler;
import CreateSableBridge.bridge.Bearing.MechanicalBearingBlockEntitySubLevelBridge;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
public abstract class MixinMechanicalBearing {

    @Shadow protected boolean isRunning;

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    private void catchAssemble(CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;
        // Делегируем сборку диспетчеру
        if (SableAssemblyHandler.tryAssemble(be)) {
            this.isRunning = true;
            ci.cancel();
        }
    }

    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void catchDisassemble(CallbackInfo ci) {
        if (!this.isRunning) return;

        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;
        // Делегируем разборку диспетчеру
        if (SableAssemblyHandler.tryDisassemble(be)) {
            this.isRunning = false;
            ci.cancel();
        }
    }

    // Сохраняем вашу логику изменения скорости (она уникальна для кинетических блоков)
    @Inject(method = "onSpeedChanged", at = @At("HEAD"))
    private void catchSpeedChange(float previousSpeed, CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;
        if (be.getLevel() != null && !be.getLevel().isClientSide()) {
            MechanicalBearingBlockEntitySubLevelBridge.updateVelocityByPos(
                    (ServerLevel) be.getLevel(), be.getBlockPos(), be.getSpeed()
            );
        }
    }

    // В классе MixinMechanicalBearing.java

    // Добавьте теневые поля, которые нужны для работы
    @Shadow protected float angle;
    @Shadow public abstract float getSpeed();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void catchTick(CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;

        // Если этот подшипник собран через Sable (записан в SavedData)
        if (this.isRunning && SableAssemblyHandler.isSableControlled(be)) {

            // 1. (Опционально, но желательно) Синхронизировать угол деревянной верхушки
            // подшипника с реальным углом поворота физического саб-уровня Sable.
            // float realAngle = SablePhysicsHandler.getAngleFromSubLevel(...);
            // this.angle = realAngle;

            // 2. Блокируем ванильный тик Create, чтобы он не пытался разобрать
            // механизм (disassemble) из-за отсутствия movedContraption
            ci.cancel();
        }
    }
}