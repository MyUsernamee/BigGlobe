package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.bigglobe.versions.RegistryVersions;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class BlockWrapper {

	public static final TypeInfo TYPE = type(Block.class);
	public static final MethodInfo GET_DEFAULT_STATE = MethodInfo.getMethod(BlockWrapper.class, "getDefaultState");
	public static final ConstantFactory CONSTANT_FACTORY = new ConstantFactory(BlockWrapper.class, "getBlock", String.class, Block.class);

	public static Block getBlock(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return getBlock(id);
	}

	public static Block getBlock(String id) {
		if (id == null) return null;
		#if MC_VERSION >= MC_1_20_3
			if (id.equals("grass") || id.equals("minecraft:grass")) {
				return Blocks.SHORT_GRASS;
			}
		#endif
		Identifier identifier = IdentifierVersions.create(id);
		if (RegistryVersions.block().containsId(identifier)) {
			return RegistryVersions.block().get(identifier);
		}
		else {
			throw new IllegalArgumentException("Unknown block: " + identifier);
		}
	}

	@SuppressWarnings("deprecation")
	public static String id(Block block) {
		return UnregisteredObjectException.getID(block.getRegistryEntry()).toString();
	}

	@SuppressWarnings("deprecation")
	public static boolean isIn(Block block, BlockTagKey key) {
		return block.getRegistryEntry().isIn(key.key());
	}

	public static BlockState getDefaultState(Block block) {
		return block.getDefaultState();
	}

	public static BlockState getRandomState(Block block, RandomGenerator random) {
		ImmutableList<BlockState> states = block.getStateManager().getStates();
		return states.get(random.nextInt(states.size()));
	}

	public BlockState getRandomState(Block block, long seed) {
		ImmutableList<BlockState> states = block.getStateManager().getStates();
		return states.get(Permuter.nextBoundedInt(seed, states.size()));
	}
}