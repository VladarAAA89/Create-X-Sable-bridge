package CreateSableBridge.mixin;

import CreateSableBridge.SableAssemblyHandler;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonBlockEntity;
import com.simibubi.create.content.contraptions.pulley.PulleyBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {MechanicalPistonBlockEntity.class, PulleyBlockEntity.class}, remap = false)
public abstract class MixinLinearActuator {

    @Shadow public boolean running;

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    private void catchAssemble(CallbackInfo ci) {
        if (SableAssemblyHandler.tryAssemble((BlockEntity) (Object) this)) {
            this.running = true;
            ci.cancel();
        }
    }

    @Inject(method = "disassemble", at = @At("HEAD"), cancellable = true)
    private void catchDisassemble(CallbackInfo ci) {
        if (!this.running) return;

        if (SableAssemblyHandler.tryDisassemble((BlockEntity) (Object) this)) {
            this.running = false;
            ci.cancel();
        }
    }
}