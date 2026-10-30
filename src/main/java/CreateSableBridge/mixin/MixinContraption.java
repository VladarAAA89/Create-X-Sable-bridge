package CreateSableBridge.mixin;

import CreateSableBridge.Interfaces.SableContraptionHandler;
import CreateSableBridge.SableContraptionHandlers;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.checkerframework.common.aliasing.qual.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = Contraption.class, remap = false)
public abstract class MixinContraption {
    @Shadow
    protected Map<BlockPos, StructureTemplate.StructureBlockInfo> blocks;
    @Unique
    private Map<BlockPos, StructureTemplate.StructureBlockInfo> createsable$snap;

    @Inject(method = "removeBlocksFromWorld", at = @At("HEAD"))
    private void createsable$before(Level world, BlockPos offset, CallbackInfo ci) {
        if (!(world instanceof ServerLevel level)) return;
        Contraption self = (Contraption) (Object) this;
        SableContraptionHandler h = SableContraptionHandlers.get(self.getType());
        if (h == null) return;
        Map<BlockPos, StructureTemplate.StructureBlockInfo> snap = new HashMap<>(blocks);
        if (h.assemble(level, self, offset)) createsable$snap = snap;
    }

    @Inject(method = "removeBlocksFromWorld", at = @At("RETURN"))
    private void createsable$after(Level world, BlockPos offset, CallbackInfo ci) {
        if (createsable$snap == null) return;
        blocks.clear();
        blocks.putAll(createsable$snap);   // Create выкинул «несовпавшие» блоки
        createsable$snap = null;
    }
}