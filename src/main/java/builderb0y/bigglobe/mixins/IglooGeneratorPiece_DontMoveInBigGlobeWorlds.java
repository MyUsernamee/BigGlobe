package builderb0y.bigglobe.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.structure.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import builderb0y.bigglobe.chunkgen.BigGlobeChunkGenerator;
import builderb0y.bigglobe.chunkgen.BigGlobeOverworldChunkGenerator;
import builderb0y.bigglobe.overriders.overworld.height.IglooHeightOverrider;

@Mixin(IglooGenerator.Piece.class)
public abstract class IglooGeneratorPiece_DontMoveInBigGlobeWorlds extends SimpleStructurePiece {

	public IglooGeneratorPiece_DontMoveInBigGlobeWorlds(
		StructurePieceType type,
		int length,
		StructureTemplateManager structureTemplateManager,
		Identifier id,
		String template,
		StructurePlacementData placementData,
		BlockPos pos
	) {
		super(type, length, structureTemplateManager, id, template, placementData, pos);
	}

	/**
	igloos spawn at Y90, always. when generate() is called, they move down to the correct position.

	why this is important: {@link IglooHeightOverrider} tries to query their bounding box before they move,
	and gets the wrong bounding box. this causes them to be placed on a hill or in a pit.
	to get around this, I would normally just move the structure via
	{@link BigGlobeChunkGenerator#moveStructure(StructureStart, int, int, int)},
	but they still move when generating, so now they are underground or floating in the sky.
	so, I now need to prevent them from moving on their own when I already moved them myself.

	alternate solutions:
	I could calculate the ideal Y inside {@link IglooHeightOverrider},
	but this will be unnecessarily slow because that runs on every column.
	*/
	@Redirect(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;"))
	private BlockPos bigglobe_dontAdd(BlockPos instance, int dx, int dy, int dz, StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator) {
		if (chunkGenerator instanceof BigGlobeOverworldChunkGenerator) {
			return instance;
		}
		else {
			return instance.add(dx, dy, dz);
		}
	}
}