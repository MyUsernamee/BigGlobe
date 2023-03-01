package builderb0y.bigglobe.trees;

import java.util.HashMap;
import java.util.Map;
import java.util.random.RandomGenerator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import builderb0y.bigglobe.blocks.BigGlobeBlocks;
import builderb0y.bigglobe.blocks.BlockStates;
import builderb0y.bigglobe.randomSources.GaussianRandomSource;

public class TreeSpecialCases {

	public static final GaussianRandomSource NATURAL_CHARRED_SLANT = new GaussianRandomSource(0.0D, 0.5D, 8);
	public static double naturalCharredSlantAmount(RandomGenerator random) {
		return NATURAL_CHARRED_SLANT.get(random.nextLong());
	}

	public static final GaussianRandomSource ARTIFICIAL_CHARRED_SLANT = new GaussianRandomSource(0.0D, 0.25D, 8);

	public static double artificialCharredSlantAmount(RandomGenerator random) {
		return ARTIFICIAL_CHARRED_SLANT.get(random.nextLong());
	}

	public static final Map<BlockState, BlockState> GROUND_REPLACEMENTS = new HashMap<>(32);
	static {
		addGroundReplacement(        Blocks.GRASS_BLOCK,      BlockStates.DIRT);
		addGroundReplacement(        Blocks.PODZOL,           BlockStates.DIRT);
		addGroundReplacement(        Blocks.MYCELIUM,         BlockStates.DIRT);
		addGroundReplacement(        Blocks.DIRT,             BlockStates.DIRT);
		addGroundReplacement(        Blocks.FARMLAND,         BlockStates.DIRT);
		addGroundReplacement(        Blocks.COARSE_DIRT,      Blocks.COARSE_DIRT.getDefaultState());
		addGroundReplacement(        Blocks.ROOTED_DIRT,      Blocks.ROOTED_DIRT.getDefaultState());
		addGroundReplacement(BigGlobeBlocks.OVERGROWN_PODZOL, BlockStates.DIRT);

		addGroundReplacement(        Blocks.SAND,             BlockStates.SAND);
		addGroundReplacement(BigGlobeBlocks.OVERGROWN_SAND,   BlockStates.SAND);

		/*
		groundReplacements.put(BlockStates.CRIMSON_NYLIUM,    BlockStates.NETHERRACK);
		groundReplacements.put(BlockStates.WARPED_NYLIUM,     BlockStates.NETHERRACK);
		groundReplacements.put(BlockStates.ASHEN_NETHERRACK,  BlockStates.NETHERRACK);
		groundReplacements.put(BlockStates.NETHERRACK,        BlockStates.NETHERRACK);

		groundReplacements.put(BlockStates.SOUL_SAND,         BlockStates.SOUL_SAND);
		groundReplacements.put(BlockStates.SOUL_SOIL,         BlockStates.SOUL_SOIL);

		groundReplacements.put(BlockStates.END_STONE,         BlockStates.END_STONE);
		*/
	}

	public static void addGroundReplacement(BlockState from, BlockState to) {
		GROUND_REPLACEMENTS.put(from, to);
	}

	public static void addGroundReplacement(Block from, BlockState to) {
		for (BlockState state : from.getStateManager().getStates()) {
			addGroundReplacement(state, to);
		}
	}

	public static Map<BlockState, BlockState> getGroundReplacements() {
		return GROUND_REPLACEMENTS;
	}

	public static void init() {}
}