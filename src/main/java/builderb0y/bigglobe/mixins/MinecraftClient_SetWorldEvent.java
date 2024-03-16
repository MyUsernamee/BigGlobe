package builderb0y.bigglobe.mixins;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.ClientWorld;

import builderb0y.bigglobe.util.ClientWorldEvents;

@Mixin(MinecraftClient.class)
public class MinecraftClient_SetWorldEvent {

	@Shadow @Nullable public ClientWorld world;

	@Inject(method = "joinWorld", at = @At("HEAD"))
	private void bigglobe_unloadOnJoinWorld(ClientWorld world, CallbackInfo callback) {
		if (this.world != null) {
			ClientWorldEvents.UNLOAD.invoker().unload(this.world);
		}
	}

	@Inject(method = "joinWorld", at = @At("TAIL"))
	private void bigglobe_loadOnJoinWorld(ClientWorld world, CallbackInfo callback) {
		ClientWorldEvents.LOAD.invoker().load(world);
	}

	@Inject(method = { "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", "enterReconfiguration" }, at = @At("HEAD"))
	private void bigglobe_unloadOnDisconnect(Screen screen, CallbackInfo callback) {
		if (this.world != null) {
			ClientWorldEvents.UNLOAD.invoker().unload(this.world);
		}
	}
}