package builderb0y.bigglobe.mixins;

import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import builderb0y.bigglobe.ClientState;

@Mixin(ClientWorld.class)
@Environment(EnvType.CLIENT)
public abstract class ClientWorld_CustomTimeSpeed extends World {

	@Unique
	private double bigglobe_customTime;

	public ClientWorld_CustomTimeSpeed(
		MutableWorldProperties properties,
		RegistryKey<World> registryRef,
		RegistryEntry<DimensionType> dimension,
		Supplier<Profiler> profiler,
		boolean isClient,
		boolean debugWorld,
		long seed,
		int maxChainedNeighborUpdates
	) {
		super(properties, registryRef, dimension, profiler, isClient, debugWorld, seed, maxChainedNeighborUpdates);
	}

	@Shadow
	public abstract void setTimeOfDay(long timeOfDay);

	@Inject(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/MutableWorldProperties;getTimeOfDay()J"), cancellable = true)
	private void bigglobe_tickTime(CallbackInfo callback) {
		this.bigglobe_customTime += ClientState.timeSpeed;
		int elapsedTicks = (int)(this.bigglobe_customTime);
		if (elapsedTicks > 0) {
			this.bigglobe_customTime -= elapsedTicks;
			this.setTimeOfDay(this.properties.getTimeOfDay() + elapsedTicks);
		}
		callback.cancel();
	}
}