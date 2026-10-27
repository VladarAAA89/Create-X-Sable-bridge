package CreateSableBridge.mixin;

import CreateSableBridge.SableAssemblyHandler;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MechanicalBearingBlockEntity.class, remap = false)
public abstract class MixinMechanicalBearing {

    @Shadow protected boolean running;
    @Shadow protected float angle;
    @Shadow protected boolean assembleNextTick;

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    private void createsable$assemble(CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;

        if (SableAssemblyHandler.tryAssemble(be)) {
            // Повторяем то, что оригинал делает в конце успешной сборки
            this.running = true;
            this.angle = 0;
            be.sendData();
            be.updateGeneratedRotation();
            ci.cancel();
        }
    }

    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void createsable$disassemble(CallbackInfo ci) {
        MechanicalBearingBlockEntity be = (MechanicalBearingBlockEntity) (Object) this;

        // Перехватываем только если именно мы собрали этот подшипник
        if (!this.running || !SableAssemblyHandler.isSableControlled(be)) return;

        if (SableAssemblyHandler.tryDisassemble(be)) {
            this.angle = 0;
            this.running = false;
            be.updateGeneratedRotation();
            this.assembleNextTick = false;   // после updateGeneratedRotation
            be.sendData();
            ci.cancel();
        }
    }
}