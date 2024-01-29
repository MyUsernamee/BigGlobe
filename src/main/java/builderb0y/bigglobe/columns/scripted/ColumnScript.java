package builderb0y.bigglobe.columns.scripted;

import java.lang.reflect.Method;

import org.objectweb.asm.Type;

import builderb0y.bigglobe.scripting.ScriptHolder;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.*;
import builderb0y.scripting.bytecode.tree.instructions.LoadInsnTree;
import builderb0y.scripting.bytecode.tree.instructions.casting.DirectCastInsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.environments.MutableScriptEnvironment;
import builderb0y.scripting.parsing.GenericScriptTemplate.GenericScriptTemplateUsage;
import builderb0y.scripting.parsing.*;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public interface ColumnScript extends Script {

	public static abstract class BaseHolder<S extends ColumnScript> extends ScriptHolder<S> {

		public BaseHolder(ScriptUsage<GenericScriptTemplateUsage> usage) {
			super(usage);
		}

		@Override
		public void compile(ColumnEntryRegistry registry) throws ScriptParsingException {
			this.script = createScript(this.usage, registry, this.getScriptClass());
		}

		public abstract Class<S> getScriptClass();

		public static <S extends ColumnScript> S createScript(ScriptUsage<GenericScriptTemplateUsage> usage, ColumnEntryRegistry registry, Class<S> type) throws ScriptParsingException {
			ClassCompileContext clazz = new ClassCompileContext(
				ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
				ClassType.CLASS,
				Type.getInternalName(type) + '$' + (usage.debug_name != null ? usage.debug_name : "Generated") + '_' + ScriptClassLoader.CLASS_UNIQUIFIER.getAndIncrement(),
				TypeInfos.OBJECT,
				new TypeInfo[] { type(type) }
			);
			clazz.addNoArgConstructor(ACC_PUBLIC);
			Method implementingMethod = ScriptParser.findImplementingMethod(type);
			TypeInfo returnType = type(implementingMethod.getReturnType());
			LazyVarInfo[] bridgeParams = new LazyVarInfo[implementingMethod.getParameterCount()];
			LazyVarInfo bridgeColumn = bridgeParams[0] = new LazyVarInfo("column", type(ScriptedColumn.class));
			LazyVarInfo y = bridgeParams.length > 1 ? (bridgeParams[1] = new LazyVarInfo("y", TypeInfos.INT)) : null;
			LazyVarInfo[] actualParams = bridgeParams.clone();
			actualParams[0] = new LazyVarInfo(actualParams[0].name, registry.columnContext.mainClass.info);
			MethodCompileContext actualMethod = clazz.newMethod(ACC_PUBLIC, implementingMethod.getName(), returnType, actualParams);
			MethodCompileContext bridgeMethod = clazz.newMethod(ACC_PUBLIC | ACC_SYNTHETIC | ACC_BRIDGE, implementingMethod.getName(), returnType, bridgeParams);

			if (y != null) {
				return_(invokeInstance(load("this", clazz.info), actualMethod.info, new DirectCastInsnTree(load(bridgeColumn), registry.columnContext.mainClass.info), load(y))).emitBytecode(bridgeMethod);
			}
			else {
				return_(invokeInstance(load("this", clazz.info), actualMethod.info, new DirectCastInsnTree(load(bridgeColumn), registry.columnContext.mainClass.info))).emitBytecode(bridgeMethod);
			}
			bridgeMethod.endCode();

			LoadInsnTree loadMainColumn = load("column", registry.columnContext.columnType());
			MutableScriptEnvironment environment = (
				new MutableScriptEnvironment()
				.addAll(MathScriptEnvironment.INSTANCE)
				.addAll(StatelessRandomScriptEnvironment.INSTANCE)
				.addVariableGetFields(loadMainColumn, ScriptedColumn.class, "x", "z", "distantHorizons")
				.addVariableRenamedGetField(loadMainColumn, "worldSeed", FieldInfo.getField(ScriptedColumn.class, "seed"))
				.addVariableRenamedInvoke(loadMainColumn, "columnSeed", MethodInfo.findMethod(ScriptedColumn.class, "columnSeed", long.class))
			);
			if (y != null) environment.addVariableLoad(y);
			registry.setupExternalEnvironment(environment, loadMainColumn);

			ScriptColumnEntryParser parser = new ScriptColumnEntryParser(usage, clazz, actualMethod).addEnvironment(environment);
			parser.parseEntireInput().emitBytecode(actualMethod);
			actualMethod.endCode();

			MethodCompileContext getSource = clazz.newMethod(ACC_PUBLIC, "getSource", TypeInfos.STRING);
			return_(ldc(usage.findSource())).emitBytecode(getSource);
			getSource.endCode();

			MethodCompileContext getDebugName = clazz.newMethod(ACC_PUBLIC, "getDebugName", TypeInfos.STRING);
			return_(ldc(usage.debug_name, TypeInfos.STRING)).emitBytecode(getDebugName);
			getDebugName.endCode();

			try {
				return type.cast(registry.loader.defineClass(clazz).getDeclaredConstructors()[0].newInstance((Object[])(null)));
			}
			catch (Throwable throwable) {
				throw new ScriptParsingException(parser.fatalError().toString(), throwable, null);
			}
		}
	}

	public static interface ColumnToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToIntScript> implements ColumnToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToIntScript> getScriptClass() {
				return ColumnToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnYToIntScript extends ColumnScript {

		public abstract int get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToIntScript> implements ColumnYToIntScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToIntScript> getScriptClass() {
				return ColumnYToIntScript.class;
			}

			@Override
			public int get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0;
				}
			}
		}
	}

	public static interface ColumnToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToLongScript> implements ColumnToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToLongScript> getScriptClass() {
				return ColumnToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
			}
		}
	}

	public static interface ColumnYToLongScript extends ColumnScript {

		public abstract long get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToLongScript> implements ColumnYToLongScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToLongScript> getScriptClass() {
				return ColumnYToLongScript.class;
			}

			@Override
			public long get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0L;
				}
			}
		}
	}

	public static interface ColumnToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToFloatScript> implements ColumnToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToFloatScript> getScriptClass() {
				return ColumnToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
			}
		}
	}

	public static interface ColumnYToFloatScript extends ColumnScript {

		public abstract float get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToFloatScript> implements ColumnYToFloatScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToFloatScript> getScriptClass() {
				return ColumnYToFloatScript.class;
			}

			@Override
			public float get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0F;
				}
			}
		}
	}

	public static interface ColumnToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToDoubleScript> implements ColumnToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToDoubleScript> getScriptClass() {
				return ColumnToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
			}
		}
	}

	public static interface ColumnYToDoubleScript extends ColumnScript {

		public abstract double get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToDoubleScript> implements ColumnYToDoubleScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToDoubleScript> getScriptClass() {
				return ColumnYToDoubleScript.class;
			}

			@Override
			public double get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return 0.0D;
				}
			}
		}
	}

	public static interface ColumnToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToBooleanScript> implements ColumnToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToBooleanScript> getScriptClass() {
				return ColumnToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface ColumnYToBooleanScript extends ColumnScript {

		public abstract boolean get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToBooleanScript> implements ColumnYToBooleanScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToBooleanScript> getScriptClass() {
				return ColumnYToBooleanScript.class;
			}

			@Override
			public boolean get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface ColumnToObjectScript extends ColumnScript {

		public abstract Object get(ScriptedColumn column);

		public static class Holder extends BaseHolder<ColumnToObjectScript> implements ColumnToObjectScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnToObjectScript> getScriptClass() {
				return ColumnToObjectScript.class;
			}

			@Override
			public Object get(ScriptedColumn column) {
				try {
					return this.script.get(column);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}

	public static interface ColumnYToObjectScript extends ColumnScript {

		public abstract Object get(ScriptedColumn column, int y);

		public static class Holder extends BaseHolder<ColumnYToObjectScript> implements ColumnYToObjectScript {

			public Holder(ScriptUsage<GenericScriptTemplateUsage> usage) {
				super(usage);
			}

			@Override
			public Class<ColumnYToObjectScript> getScriptClass() {
				return ColumnYToObjectScript.class;
			}

			@Override
			public Object get(ScriptedColumn column, int y) {
				try {
					return this.script.get(column, y);
				}
				catch (Throwable throwable) {
					this.onError(throwable);
					return false;
				}
			}
		}
	}
}