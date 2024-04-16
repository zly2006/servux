package fi.dy.masa.servux.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.servux.dataproviders.StructureDataProvider;

@Mixin(ServerWorld.class)
public class MixinServerWorld
{
    @Shadow private int spawnChunkRadius;

    @Inject(method = "setSpawnPos", at = @At("TAIL"))
    private void checkSpawnPos(BlockPos pos, float angle, CallbackInfo ci)
    {
        if (StructureDataProvider.INSTANCE.getSpawnPos() != pos)
        {
            StructureDataProvider.INSTANCE.setSpawnPos(pos);
        }
        if (StructureDataProvider.INSTANCE.getSpawnChunkRadius() != (this.spawnChunkRadius - 1))
        {
            StructureDataProvider.INSTANCE.setSpawnChunkRadius((this.spawnChunkRadius - 1));
        }
    }
}
