package builderb0y.bigglobe.structures.scripted;

import java.util.random.RandomGenerator;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructurePiece;

import builderb0y.bigglobe.columns.ColumnValue;
import builderb0y.bigglobe.columns.WorldColumn;
import builderb0y.bigglobe.columns.scripted.ColumnEntryRegistry;
import builderb0y.bigglobe.scripting.*;
import builderb0y.bigglobe.scripting.environments.*;
import builderb0y.bigglobe.scripting.wrappers.BiomeEntry;
import builderb0y.bigglobe.scripting.wrappers.StructurePlacementScriptEntry;
import builderb0y.bigglobe.util.CheckedList;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.Handlers;
import builderb0y.scripting.environments.JavaUtilScriptEnvironment;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment.FunctionHandler;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.Script;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.parsing.ScriptUsage;
import builderb0y.scripting.parsing.TemplateScriptParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface StructureLayoutScript extends Script {

	public abstract void layout(
		int originX,
		int originZ,
		RandomGenerator random,
		WorldColumn column,
		CheckedList<StructurePiece> pieces,
		boolean distantHorizons
	);

	public static class Holder extends ScriptHolder<StructureLayoutScript> implements StructureLayoutScript {

		public static final InsnTree LOAD_RANDOM = load("random", type(RandomGenerator.class));

		public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = (
				new TemplateScriptParser<>(StructureLayoutScript.class, usage)
				.addEnvironment(JavaUtilScriptEnvironment.withRandom(LOAD_RANDOM))
				.addEnvironment(MathScriptEnvironment.INSTANCE)
				.addEnvironment(RandomScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE)
				.addEnvironment(StructureScriptEnvironment.INSTANCE)
				.addEnvironment(NbtScriptEnvironment.INSTANCE)
				.addEnvironment(WoodPaletteScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(MinecraftScriptEnvironment.createWithRandom(LOAD_RANDOM))
				.addEnvironment(SymmetryScriptEnvironment.create(LOAD_RANDOM))
				.addEnvironment(
					new MutableScriptEnvironment()

					.addVariableLoad("originX", TypeInfos.INT)
					.addVariableLoad("originZ", TypeInfos.INT)

					.addVariable(
						"worldSeed",
						getField(
							load("column", type(WorldColumn.class)),
							ColumnScriptEnvironmentBuilder.SEED
						)
					)

					.addFunction(
						"getBiome",
						new FunctionHandler.Named(
							"getBiomeFromColumn(int x, int y, int z)",
							Handlers
							.builder(Holder.class, "getBiome")
							.addImplicitArgument(load("column", type(WorldColumn.class)))
							.addArguments("III")
							.buildFunction()
						)
					)

					.addQualifiedSpecificConstructor(ScriptedStructure.Piece.class, int.class, int.class, int.class, int.class, int.class, int.class, StructurePlacementScriptEntry.class, NbtCompound.class)
					.addMethodInvokes(ScriptedStructure.Piece.class, "withRotation", "rotateAround", "symmetrify", "symmetrifyAround", "offset")
					.addMethod(type(ScriptedStructure.Piece.class), "rotateRandomly", Handlers.builder(ScriptedStructure.Piece.class, "rotateRandomly").addReceiverArgument(ScriptedStructure.Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
					.addMethod(type(ScriptedStructure.Piece.class), "rotateAndFlipRandomly", Handlers.builder(ScriptedStructure.Piece.class, "rotateAndFlipRandomly").addReceiverArgument(ScriptedStructure.Piece.class).addImplicitArgument(LOAD_RANDOM).buildMethod())
					.addFieldGet(ScriptedStructure.Piece.class, "data")
					.addType("ScriptStructurePlacement", StructurePlacementScriptEntry.class)

					.addVariableLoad("pieces", type(CheckedList.class))

					.addVariableLoad("distantHorizons", TypeInfos.BOOLEAN)
				)
				.addEnvironment(
					ColumnScriptEnvironmentBuilder.createVariableXYZ(
						ColumnValue.REGISTRY,
						load("column", type(WorldColumn.class))
					)
					.build()
				)
				.parse()
			);
		}

		@Override
		public void layout(
			int originX,
			int originZ,
			RandomGenerator random,
			WorldColumn column,
			CheckedList<StructurePiece> pieces,
			boolean distantHorizons
		) {
			try {
				this.script.layout(originX, originZ, random, column, pieces, distantHorizons);
			}
			catch (Throwable throwable) {
				this.onError(throwable);
			}
		}

		public static BiomeEntry getBiome(WorldColumn column, int x, int y, int z) {
			column.setPos(x, z);
			return new BiomeEntry(column.getBiome(y));
		}
	}
}