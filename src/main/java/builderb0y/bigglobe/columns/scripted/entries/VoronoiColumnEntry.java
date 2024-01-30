package builderb0y.bigglobe.columns.scripted.entries;

import java.lang.invoke.*;
import java.util.*;
import java.util.stream.Collectors;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

import builderb0y.autocodec.annotations.VerifyNullable;
import builderb0y.autocodec.util.AutoCodecUtil;
import builderb0y.autocodec.verifiers.VerifyContext;
import builderb0y.autocodec.verifiers.VerifyException;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn;
import builderb0y.bigglobe.columns.scripted.ScriptedColumn.VoronoiDataBase;
import builderb0y.bigglobe.columns.scripted.Valid;
import builderb0y.bigglobe.columns.scripted.VoronoiSettings;
import builderb0y.bigglobe.columns.scripted.compile.*;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.AccessSchema.AccessContext;
import builderb0y.bigglobe.columns.scripted.types.VoronoiColumnValueType;
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

public class VoronoiColumnEntry extends AbstractColumnEntry {

	public static final ColumnEntryMemory.Key<Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext>>
		VORONOI_CONTEXT_MAP = new ColumnEntryMemory.Key<>("voronoiContextMap");
	public static final ColumnEntryMemory.Key<Map<MemoryMapLookup, ColumnEntryMemory>>
		MEMORY_MAP = new ColumnEntryMemory.Key<>("memoryMap");
	public static record MemoryMapLookup(RegistryEntry<VoronoiSettings> voronoiSettings, RegistryEntry<ColumnEntry> columnEntry) {}

	public static final MethodHandle RANDOMIZE;
	static {
		try {
			RANDOMIZE = MethodHandles.lookup().findStatic(VoronoiColumnEntry.class, "randomize", MethodType.methodType(VoronoiDataBase.class, RandomList.class, long.class, ScriptedColumn.class, Cell.class));
		}
		catch (Exception exception) {
			throw AutoCodecUtil.rethrow(exception);
		}
	}
	public static final ColumnEntryMemory.Key<List<RegistryEntry<VoronoiSettings>>>
		OPTIONS = new ColumnEntryMemory.Key<>("options");

	public final VoronoiDiagram2D diagram;

	public VoronoiColumnEntry(
		VoronoiDiagram2D diagram,
		AccessSchema params,
		@VerifyNullable Valid valid
	) {
		super(params, valid, true);
		this.diagram = diagram;
		if (!(params.type() instanceof VoronoiColumnValueType)) {
			throw new IllegalArgumentException("params.type must be 'bigglobe:voronoi' when column value type is 'bigglobe:voronoi'");
		}
		if (params.is_3d()) {
			throw new IllegalArgumentException("params.is_3d must be false when column value is 'bigglobe:voronoi'");
		}
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
				LambdaMetafactory.altMetafactory(
					lookup,
					"create",
					MethodType.methodType(VoronoiDataBase.Factory.class),
					MethodType.methodType(VoronoiDataBase.class, lookup.lookupClass(), VoronoiDiagram2D.Cell.class),
					lookup.findConstructor(option, MethodType.methodType(void.class, lookup.lookupClass(), VoronoiDiagram2D.Cell.class)),
					MethodType.methodType(option, lookup.lookupClass(), VoronoiDiagram2D.Cell.class),
					LambdaMetafactory.FLAG_BRIDGES,
					1,
					MethodType.methodType(VoronoiDataBase.class, ScriptedColumn.class, VoronoiDiagram2D.Cell.class)
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

	public static VoronoiDataBase randomize(RandomList<VoronoiDataBase.Factory> factories, long baseSeed, ScriptedColumn column, VoronoiDiagram2D.Cell cell) {
		return factories.isEmpty() ? null : factories.getRandomElement(cell.center.getSeed(baseSeed)).create(column, cell);
	}

	@Override
	public boolean hasField() {
		return true;
	}

	@Override
	public boolean isSettable() {
		return false;
	}

	public Map<String, AccessSchema> exports() {
		return ((VoronoiColumnValueType)(this.params.type())).exports;
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
			if (!this.exports().equals(expected)) {
				throw new IllegalStateException("Export mismatch between column value " + memory.getTyped(ColumnEntryMemory.ACCESSOR_ID) + ' ' + this.exports() + " and voronoi settings " + UnregisteredObjectException.getID(preset) + ' ' + expected);
			}
		}

		AccessContext baseType = memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT);
		VoronoiBaseCompileContext voronoiBaseContext = (VoronoiBaseCompileContext)(Objects.requireNonNull(baseType.context()));
		voronoiBaseContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "SEED", TypeInfos.LONG).node.value = Permuter.permute(0L, memory.getTyped(ColumnEntryMemory.ACCESSOR_ID));

		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = new HashMap<>();
		memory.putTyped(VORONOI_CONTEXT_MAP, voronoiContextMap);
		for (Map.Entry<String, AccessSchema> entry : this.exports().entrySet()) {
			voronoiBaseContext.mainClass.newMethod(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), context.root().getTypeContext(entry.getValue().type()).type(), entry.getValue().getterParameters());
		}
		Map<MemoryMapLookup, ColumnEntryMemory> memoryMap = new HashMap<>();
		memory.putTyped(MEMORY_MAP, memoryMap);
		for (RegistryEntry<VoronoiSettings> voronoiEntry : options) {
			VoronoiImplCompileContext implContext = new VoronoiImplCompileContext(voronoiBaseContext, voronoiEntry);
			voronoiContextMap.put(UnregisteredObjectException.getKey(voronoiEntry), implContext);
			implContext.mainClass.newField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL, "WEIGHT", TypeInfos.DOUBLE).node.value = voronoiEntry.value().weight();

			for (RegistryEntry<ColumnEntry> enable : voronoiEntry.value().enables()) {
				ColumnEntryMemory enabledMemory = new ColumnEntryMemory(enable);
				AccessSchema accessSchema = enable.value().getAccessSchema();
				enabledMemory.putTyped(ColumnEntryMemory.TYPE_CONTEXT, context.root().getTypeContext(accessSchema.type()));
				enabledMemory.putTyped(ColumnEntryMemory.ACCESS_CONTEXT, context.root().getAccessContext(accessSchema));
				if (memoryMap.putIfAbsent(new MemoryMapLookup(voronoiEntry, enable), enabledMemory) != null) {
					throw new IllegalStateException("old already in memoryMap");
				}
				enable.value().emitFieldGetterAndSetter(enabledMemory, implContext);
			}

			for (Map.Entry<String, RegistryEntry<ColumnEntry>> export : voronoiEntry.value().exports().entrySet()) {
				ColumnEntryMemory exportMemory = memoryMap.get(new MemoryMapLookup(voronoiEntry, export.getValue()));
				if (exportMemory == null) {
					exportMemory = Objects.requireNonNull(
						context.root().registry.memories.get(
							UnregisteredObjectException.getID(export.getValue())
						)
					);
				}
				AccessSchema schema = export.getValue().value().getAccessSchema();
				MethodCompileContext delegator = implContext.mainClass.newMethod(ACC_PUBLIC, "get_" + export.getKey(), implContext.root().getTypeContext(schema.type()).type(), schema.getterParameters());
				LazyVarInfo self = new LazyVarInfo("this", delegator.clazz.info);
				LazyVarInfo loadY = schema.is_3d() ? new LazyVarInfo("y", TypeInfos.INT) : null;
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
	public void setupEnvironment(ColumnEntryMemory memory, DataCompileContext context, InsnTree loadHolder) {
		super.setupEnvironment(memory, context, loadHolder);
		context.environment.addType(((VoronoiColumnValueType)(this.params.type())).name, context.root().getTypeContext(this.params.type()).type());
		if (context.parent == null) {
			for (Map.Entry<String, AccessSchema> entry : this.exports().entrySet()) {
				context.addAccessor(loadHolder, entry.getKey(), entry.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), context));
			}
		}
		List<RegistryEntry<VoronoiSettings>> options = memory.getTyped(OPTIONS);
		Map<RegistryKey<VoronoiSettings>, VoronoiImplCompileContext> voronoiContextMap = memory.getTyped(VORONOI_CONTEXT_MAP);
		Map<MemoryMapLookup, ColumnEntryMemory> memoryMap = memory.getTyped(MEMORY_MAP);
		for (RegistryEntry<VoronoiSettings> voronoiEntry : options) {
			VoronoiImplCompileContext implContext = Objects.requireNonNull(voronoiContextMap.get(UnregisteredObjectException.getKey(voronoiEntry)));
			InsnTree loadImplContext = implContext.loadSelf();
			for (RegistryEntry<ColumnEntry> enable : voronoiEntry.value().enables()) {
				ColumnEntryMemory enabledMemory = memoryMap.get(new MemoryMapLookup(voronoiEntry, enable));
				if (enabledMemory == null) {
					throw new IllegalStateException("memoryMap does not contain " + voronoiEntry + " / " + enable);
				}
				enable.value().setupEnvironment(enabledMemory, implContext, loadImplContext);
			}
			InsnTree loadColumn = implContext.loadColumn();
			for (ColumnEntryMemory filteredMemory : context.root().registry.filteredMemories) {
				ColumnEntry columnEntry = filteredMemory.getTyped(ColumnEntryMemory.ENTRY);
				if (!(columnEntry instanceof VoronoiColumnEntry)) {
					columnEntry.setupEnvironment(filteredMemory, implContext, loadColumn);
				}
			}
		}
	}

	@Override
	public void setupExternalEnvironment(ColumnEntryMemory memory, ColumnCompileContext context, MutableScriptEnvironment environment, ExternalEnvironmentParams params) {
		super.setupExternalEnvironment(memory, context, environment, params);
		DataCompileContext selfContext = context.root().getAccessContext(this.getAccessSchema()).context();
		for (Map.Entry<String, AccessSchema> entry : this.exports().entrySet()) {
			MethodInfo method = entry.getValue().getterDescriptor(ACC_PUBLIC | ACC_ABSTRACT, "get_" + entry.getKey(), selfContext);
			if (entry.getValue().is_3d()) {
				environment.addMethodInvoke(entry.getKey(), method);
			}
			else {
				environment.addFieldInvoke(entry.getKey(), method);
			}
		}
	}

	@Override
	public void populateCompute2D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		ConstantValue diagram = ConstantValue.ofManual(this.diagram, type(VoronoiDiagram2D.class));
		FieldCompileContext valueField = memory.getTyped(ColumnEntryMemory.FIELD);
		FieldInfo cellField = FieldInfo.getField(VoronoiDataBase.class, "cell");
		InsnTree self = context.loadSelf();
		return_(
			invokeDynamic(
				MethodInfo.getMethod(VoronoiColumnEntry.class, "createRandomizer"),
				new MethodInfo(
					ACC_PUBLIC | ACC_STATIC,
					TypeInfos.OBJECT, //ignored.
					"randomize",
					memory.getTyped(ColumnEntryMemory.ACCESS_CONTEXT).exposedType(),
					context.root().columnType(),
					type(VoronoiDiagram2D.Cell.class)
				),
				memory.getTyped(VORONOI_CONTEXT_MAP).values().stream().map((DataCompileContext impl) -> constant(impl.mainClass.info)).toArray(ConstantValue[]::new),
				new InsnTree[] {
					self,
					invokeInstance(
						ldc(diagram),
						MethodInfo.findMethod(VoronoiDiagram2D.class, "getNearestCell", VoronoiDiagram2D.Cell.class, int.class, int.class, VoronoiDiagram2D.Cell.class),
						getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "x")),
						getField(context.loadColumn(), FieldInfo.getField(ScriptedColumn.class, "z")),
						new NullableInstanceGetFieldInsnTree(
							getField(
								self,
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
		Map<MemoryMapLookup, ColumnEntryMemory> memoryMap = memory.getTyped(MEMORY_MAP);
		for (RegistryEntry<VoronoiSettings> voronoiEntry : options) {
			VoronoiImplCompileContext implContext = Objects.requireNonNull(voronoiContextMap.get(UnregisteredObjectException.getKey(voronoiEntry)));
			for (RegistryEntry<ColumnEntry> enable : voronoiEntry.value().enables()) {
				ColumnEntryMemory enabledMemory = memoryMap.get(new MemoryMapLookup(voronoiEntry, enable));
				if (enabledMemory == null) {
					throw new IllegalStateException(voronoiEntry + " / " + enable + " not in " + memoryMap);
				}
				enable.value().emitComputer(enabledMemory, implContext);
			}
		}
	}

	@Override
	public void populateCompute3D(ColumnEntryMemory memory, DataCompileContext context, MethodCompileContext computeMethod) throws ScriptParsingException {
		throw new UnsupportedOperationException();
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