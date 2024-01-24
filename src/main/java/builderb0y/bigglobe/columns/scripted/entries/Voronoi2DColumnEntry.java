package builderb0y.bigglobe.columns.scripted.entries;

import java.lang.invoke.*;
import java.util.*;
import java.util.stream.Collectors;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.DefaultEmpty;
import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseVerifier;
import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.*;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema;
import builderb0y.bigglobe.columns.scripted.schemas.AccessSchema.TypeContext;
import builderb0y.bigglobe.columns.scripted.schemas.Voronoi2DAccessSchema;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiBaseCompileContext;
import builderb0y.bigglobe.columns.scripted.compile.VoronoiImplCompileContext;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.Valids.NullObject2DValid;
import builderb0y.bigglobe.columns.scripted.Valids._2DValid;
import builderb0y.bigglobe.noise.Permuter;
import builderb0y.bigglobe.randomLists.RandomList;
import builderb0y.bigglobe.settings.VoronoiDiagram2D;
import builderb0y.bigglobe.settings.VoronoiDiagram2D.Cell;
import builderb0y.bigglobe.util.UnregisteredObjectException;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.instructions.fields.NullableInstanceGetFieldInsnTree;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class Voronoi2DColumnEntry extends Basic2DColumnEntry {

	public static final ColumnEntryMemory.Key<Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext>>
		VORONOI_CONTEXT_MAP = new ColumnEntryMemory.Key<>("voronoiContextMap");
	public static final MethodHandle RANDOMIZE;
	static {
		try {
			RANDOMIZE = MethodHandles.lookup().findStatic(Voronoi2DColumnEntry.class, "randomize", MethodType.methodType(VoronoiDataBase.class, RandomList.class, long.class, Cell.class));
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}
	public static final ColumnEntryMemory.Key<List<RegistryEntry<VoronoiSettings>>>
		OPTIONS = new ColumnEntryMemory.Key<>("options");

	public final VoronoiDiagram2D value;
	public final @DefaultEmpty Map<@UseVerifier(name = "checkNotReserved", in = Voronoi2DColumnEntry.class, usage = MemberUsage.METHOD_IS_HANDLER) String, AccessSchema> exports;
	public final @VerifyNullable NullObject2DValid valid;

	public Voronoi2DColumnEntry(
		VoronoiDiagram2D value,
		Map<String, AccessSchema> exports,
		@VerifyNullable NullObject2DValid valid
	) {
		this.value   = value;
		this.exports = exports;
		this.valid   = valid;
	}

	@Override
	public _2DValid valid() {
		return this.valid;
	}

	public static CallSite createRandomizer(MethodHandles.Lookup lookup, String name, MethodType methodType, Class<?>... options) throws Throwable {
		if (methodType.returnType().getSuperclass() != VoronoiDataBase.class) {
			throw new IllegalArgumentException("Invalid super class: " + methodType.returnType().getSuperclass());
		}
		long seed = methodType.returnType().getDeclaredField("SEED").getLong(null);
		RandomList<VoronoiDataBase.Factory> list = new RandomList<>(options.length);
		for (Class<?> option : options) {
			option.asSubclass(VoronoiDataBase.class);
			VoronoiDataBase.Factory factory = (VoronoiDataBase.Factory)(
				LambdaMetafactory.metafactory(
					lookup,
					"create",
					MethodType.methodType(VoronoiDataBase.Factory.class),
					MethodType.methodType(VoronoiDataBase.class, VoronoiDiagram2D.Cell.class),
					lookup.findConstructor(option, MethodType.methodType(void.class, VoronoiDiagram2D.Cell.class)),
					MethodType.methodType(option, VoronoiDiagram2D.Cell.class)
				)
				.getTarget()
				.invokeExact()
			);
			double weight = option.getDeclaredField("WEIGHT").getDouble(null);
			list.add(factory, weight);
		}
		return new ConstantCallSite(
			MethodHandles
			.insertArguments(RANDOMIZE, 0, list, seed)
			.asType(methodType)
		);
	}

	public static VoronoiDataBase randomize(RandomList<VoronoiDataBase.Factory> factories, long baseSeed, VoronoiDiagram2D.Cell cell) {
		return factories.isEmpty() ? null : factories.getRandomElement(cell.center.getSeed(baseSeed)).create(cell);
	}

	@Override
	public boolean hasField() {
		return true;
	}

	@Override
	public boolean isSettable() {
		return false;
	}

	@Override
	public AccessSchema getAccessSchema() {
		return new Voronoi2DAccessSchema(this.exports);
	}

	@Override
	public void populateSetter(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext setterMethod) {

	}

	@Override
	public void emitFieldGetterAndSetter(ColumnEntryMemory memory, DataCompileContext context) {
		super.emitFieldGetterAndSetter(memory, context);

		List<RegistryEntry<VoronoiSettings>> options = memory.addOrGet(OPTIONS, () -> new ArrayList<>(0));
		//sanity check that all implementations of this class export the same values we do.
		for (RegistryEntry<VoronoiSettings> preset : options) {
			Map<String, AccessSchema> expected = preset.value().exports().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (Map.Entry<String, RegistryEntry<ColumnEntry>> entry) -> entry.getValue().value().getAccessSchema()));
			if (!this.exports.equals(expected)) {
				throw new IllegalStateException("Export mismatch between column value " + memory.getTyped(ColumnEntryMemory.ACCESSOR_ID) + ' ' + this.exports + " and voronoi settings " + UnregisteredObjectException.getID(preset) + ' ' + expected);
			}
		}

		TypeContext baseType = context.root().getSchemaType(this.getAccessSchema());
		VoronoiBaseCompileContext voronoiBaseContext = (VoronoiBaseCompileContext)(Objects.requireNonNull(baseType.context()));
		voronoiBaseContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "SEED", TypeInfos.LONG).node.value = Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));

		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = new HashMap<>();
		memory.putTyped(VORONOI_CONTEXT_MAP, voronoiContextMap);
		for (Map.Entry<String, AccessSchema> entry : this.exports.entrySet()) {
			voronoiBaseContext.mainClass.newMethod(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), voronoiBaseContext.selfType(), entry.getValue().getterParameters());
		}
		for (RegistryEntry<VoronoiSettings> entry : options) {
			VoronoiImplCompileContext implContext = new VoronoiImplCompileContext(voronoiBaseContext, Permuter.permute(0L, UnregisteredObjectException.getID(entry)));
			voronoiContextMap.put(UnregisteredObjectException.getKey(entry), implContext);
			implContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "WEIGHT", TypeInfos.DOUBLE).node.value = entry.value().weight();

			for (RegistryEntry<ColumnEntry> enable : entry.value().enables()) {
				ColumnEntryMemory enabledMemory = Objects.requireNonNull(
					context.root().registry.memories.get(
						UnregisteredObjectException.getID(enable)
					)
				);
				enable.value().emitFieldGetterAndSetter(enabledMemory, implContext);
			}

			for (Map.Entry<String, RegistryEntry<ColumnEntry>> export : entry.value().exports().entrySet()) {
				ColumnEntryMemory exportMemory = Objects.requireNonNull(
					context.root().registry.memories.get(
						UnregisteredObjectException.getID(export.getValue())
					)
				);
				AccessSchema schema = export.getValue().value().getAccessSchema();
				MethodCompileContext delegator = implContext.mainClass.newMethod(ACC_PUBLIC, "get_" + export.getKey(), implContext.selfType(), schema.getterParameters());
				LazyVarInfo self = new LazyVarInfo("this", delegator.clazz.info);
				LazyVarInfo loadY = schema.is3D() ? new LazyVarInfo("y", TypeInfos.INT) : null;
				return_(
					invokeInstance(
						load(self),
						exportMemory.getTyped(ColumnEntryMemory.GETTER).info,
						loadY != null ? new InsnTree[] { load(loadY) } : InsnTree.ARRAY_FACTORY.empty()
					)
				)
				.emitBytecode(delegator);
				delegator.endCode();
			}
		}
	}

	@Override
	public void setupEnvironment(ColumnEntryMemory memory, DataCompileContext context) {
		super.setupEnvironment(memory, context);
		for (Map.Entry<String, AccessSchema> entry : this.exports.entrySet()) {
			context.environment.addVariableRenamedInvoke(context.loadSelf(), entry.getKey(), entry.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), context));
		}
		List<RegistryEntry<VoronoiSettings>> options = memory.getTyped(OPTIONS);
		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = memory.getTyped(VORONOI_CONTEXT_MAP);
		for (RegistryEntry<VoronoiSettings> entry : options) {
			VoronoiImplCompileContext implContext = Objects.requireNonNull(voronoiContextMap.get(UnregisteredObjectException.getKey(entry)));
			for (RegistryEntry<ColumnEntry> enable : entry.value().enables()) {
				ColumnEntryMemory enabledMemory = Objects.requireNonNull(
					context.root().registry.memories.get(
						UnregisteredObjectException.getID(enable)
					)
				);
				enable.value().setupEnvironment(enabledMemory, implContext);
			}
		}
	}

	@Override
	public void setupExternalEnvironment(ColumnEntryMemory memory, DataCompileContext context, MutableScriptEnvironment environment, InsnTree loadColumn) {
		super.setupExternalEnvironment(memory, context, environment, loadColumn);
		for (Map.Entry<String, AccessSchema> entry : this.exports.entrySet()) {
			environment.addMethodInvoke(entry.getKey(), entry.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), context));
		}
	}

	@Override
	public void populateCompute(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		ConstantValue diagram = ConstantValue.ofManual(this.value, type(VoronoiDiagram2D.class));
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		FieldInfo cellField = FieldInfo.getField(VoronoiDataBase.class, "cell");
		LazyVarInfo self = new LazyVarInfo("this", computeMethod.clazz.info);
		return_(
			invokeDynamic(
				MethodInfo.getMethod(Voronoi2DColumnEntry.class, "createRandomizer"),
				new MethodInfo(
					ACC_PUBLIC | ACC_STATIC,
					TypeInfos.OBJECT, //ignored.
					"randomize",
					memory.getTyped(ColumnEntryMemory.TYPE).exposedType(),
					type(VoronoiDiagram2D.Cell.class)
				),
				memory.getTyped(VORONOI_CONTEXT_MAP).values().stream().map((DataCompileContext impl) -> constant(impl.mainClass.info)).toArray(ConstantValue[]::new),
				new InsnTree[] {
					invokeInstance(
						ldc(diagram),
						MethodInfo.findMethod(VoronoiDiagram2D.class, "getNearestCell", VoronoiDiagram2D.Cell.class, int.class, int.class, VoronoiDiagram2D.Cell.class),
						getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "x")),
						getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "z")),
						new NullableInstanceGetFieldInsnTree(
							getField(
								load(self),
								valueField.info
							),
							cellField
						)
					)
				}
			)
		)
		.emitBytecode(computeMethod);
		computeMethod.endCode();

		List<RegistryEntry<VoronoiSettings>> options = memory.getTyped(OPTIONS);
		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = memory.getTyped(VORONOI_CONTEXT_MAP);
		for (RegistryEntry<VoronoiSettings> entry : options) {
			VoronoiImplCompileContext implContext = Objects.requireNonNull(voronoiContextMap.get(UnregisteredObjectException.getKey(entry)));
			for (RegistryEntry<ColumnEntry> enable : entry.value().enables()) {
				ColumnEntryMemory enabledMemory = Objects.requireNonNull(
					context.root().registry.memories.get(
						UnregisteredObjectException.getID(enable)
					)
				);
				enable.value().emitComputer(enabledMemory, implContext);
			}
		}
	}

	public static <T> void checkNotReserved(VerifyContext<T, String> context) throws VerifyException {
		String name = context.object;
		if (name != null) switch (name) {
			case
				"cell_x",
				"cell_z",
				"center_x",
				"center_z",
				"soft_distance_squared",
				"soft_distance",
				"hard_distance",
				"hard_distance_squared",
				"euclidean_distance_squared",
				"euclidean_distance"
			-> {
				throw new VerifyException(() -> "Export name " + name + " is built-in, and cannot be overridden.");
			}
		}
	}
}