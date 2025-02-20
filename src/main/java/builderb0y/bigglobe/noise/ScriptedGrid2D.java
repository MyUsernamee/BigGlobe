package builderb0y.bigglobe.noise;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import builderb0y.autocodec.annotations.MultiLine;
import builderb0y.bigglobe.scripting.environments.StatelessRandomScriptEnvironment;
import builderb0y.scripting.bytecode.LazyVarInfo;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.ScopeContext.LoopName;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.environments.MathScriptEnvironment;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class ScriptedGrid2D extends ScriptedGrid<Grid2D> implements Grid2D {

	public static final GridTypeInfo GRID_2D_TYPE_INFO = new GridTypeInfo(Grid2D.class, Parser.class, 2);

	public final transient Grid2D delegate;

	public ScriptedGrid2D(@MultiLine String script, Map<String, Grid2D> inputs, double min, double max) throws ScriptParsingException {
		super(script, inputs, min, max);
		LinkedHashMap<String, Input> processedInputs = processInputs(inputs, GRID_2D_TYPE_INFO);
		Parser parser = new Parser(script, processedInputs);
		parser
		.addEnvironment(new Environment(processedInputs, GRID_2D_TYPE_INFO))
		.addEnvironment(MathScriptEnvironment.INSTANCE)
		.addEnvironment(StatelessRandomScriptEnvironment.INSTANCE);
		this.delegate = parser.parse();
	}

	@Override
	public Grid getDelegate() {
		return this.delegate;
	}

	@Override
	public double getValue(long seed, int x, int y) {
		return this.delegate.getValue(seed, x, y);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, NumberArray samples) {
		//workaround for the fact that I *really* don't want to deal
		//with generating bytecode for try-with-resources at runtime.
		NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
		int used = manager.used;
		try {
			this.delegate.getBulkX(seed, startX, y, samples);
		}
		catch (Throwable throwable) {
			this.onError(throwable);
		}
		finally {
			manager.used = used;
		}
	}

	@Override
	public void getBulkY(long seed, int x, int startY, NumberArray samples) {
		//workaround for the fact that I *really* don't want to deal
		//with generating bytecode for try-with-resources at runtime.
		NumberArray.Manager manager = NumberArray.Manager.INSTANCES.get();
		int used = manager.used;
		try {
			this.delegate.getBulkY(seed, x, startY, samples);
		}
		catch (Throwable throwable) {
			this.onError(throwable);
		}
		finally {
			manager.used = used;
		}
	}

	public static class Parser extends ScriptedGrid.Parser<Grid2D> {

		@SuppressWarnings("MultipleVariablesInDeclaration")
		public static final MethodInfo
			GET_BULK_X = MethodInfo.getMethod(Grid2D.class, "getBulkX"),
			GET_BULK_Y = MethodInfo.getMethod(Grid2D.class, "getBulkY"),
			GET_BULK[] = { GET_BULK_X, GET_BULK_Y };

		public Parser(@MultiLine String script, LinkedHashMap<String, Input> inputs) {
			super(script, inputs, GRID_2D_TYPE_INFO);
		}

		@Override
		public void addGetBulkOne(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			LazyVarInfo self, seed, x, y, samples, sampleCount;
			MethodCompileContext getBulk = this.clazz.newMethod(
				ACC_PUBLIC,
				methodInfo.name,
				TypeInfos.VOID,
				seed = new LazyVarInfo("seed", TypeInfos.LONG),
				x = new LazyVarInfo("x", TypeInfos.INT),
				y = new LazyVarInfo("y", TypeInfos.INT),
				samples = new LazyVarInfo("samples", NUMBER_ARRAY_TYPE)
			);
			self = new LazyVarInfo("this", getBulk.clazz.info);
			sampleCount = getBulk.scopes.addVariable("sampleCount", TypeInfos.INT);
			Input firstInput = this.gridInputs.values().iterator().next();

			//sampleCount = samples.length();
			store(sampleCount, numberArrayLength(load(samples))).emitBytecode(getBulk);
			//if (sampleCount <= 0) return;
			ifThen(
				le(
					this,
					load(sampleCount),
					ldc(0)
				),
				return_(noop)
			)
			.emitBytecode(getBulk);
			//fill samples with firstInput.
			invokeInstance(
				getField(
					load(self),
					firstInput.fieldInfo(getBulk)
				),
				methodInfo,
				load(seed),
				load(x),
				load(y),
				load(samples)
			)
			.emitBytecode(getBulk);
			getBulk.node.visitLabel(label());
			//replace samples with evaluation results.
			getBulk.scopes.pushScope();
			LazyVarInfo index = getBulk.scopes.addVariable("index", TypeInfos.INT);
			for_(
				new LoopName(null),
				store(index, ldc(0)),
				lt(
					this,
					load(index),
					load(sampleCount)
				),
				inc(index, 1),
				numberArrayStore(
					load(samples),
					load(index),
					invokeStatic(
						new MethodInfo(
							ACC_PUBLIC | ACC_STATIC,
							getBulk.clazz.info,
							"evaluate",
							TypeInfos.DOUBLE,
							types('I', 'I', 'D', this.gridInputs.size())
						),
						maybeAdd(this, x, index, 0, methodDimension),
						maybeAdd(this, y, index, 1, methodDimension),
						numberArrayLoad(load(samples), load(index))
					)
				)
			)
			.emitBytecode(getBulk);
			getBulk.scopes.popScope();
			//return.
			return_(noop).emitBytecode(getBulk);
			getBulk.endCode();
		}

		@Override
		public void addGetBulkMany(int methodDimension) {
			MethodInfo methodInfo = GET_BULK[methodDimension];
			LazyVarInfo self, seed, x, y, samples, sampleCount;
			MethodCompileContext getBulk = this.clazz.newMethod(
				ACC_PUBLIC,
				methodInfo.name,
				TypeInfos.VOID,
				seed = new LazyVarInfo("seed", TypeInfos.LONG),
				x = new LazyVarInfo("x", TypeInfos.INT),
				y = new LazyVarInfo("y", TypeInfos.INT),
				samples = new LazyVarInfo("samples", NUMBER_ARRAY_TYPE)
			);
			self = new LazyVarInfo("this", getBulk.clazz.info);
			sampleCount = getBulk.scopes.addVariable("sampleCount", TypeInfos.INT);

			//declare scratch arrays.
			LazyVarInfo[] scratches = new LazyVarInfo[this.gridInputs.size()];
			for (Input input : this.gridInputs.values()) {
				scratches[input.index] = getBulk.scopes.addVariable(input.name, NUMBER_ARRAY_TYPE);
			}
			//sampleCount = samples.length();
			store(sampleCount, numberArrayLength(load(samples))).emitBytecode(getBulk);
			//if (sampleCount <= 0) return;
			ifThen(
				le(this, load(sampleCount), ldc(0)),
				return_(noop)
			)
			.emitBytecode(getBulk);
			//allocate scratch arrays.
			for (Input input : this.gridInputs.values()) {
				store(
					scratches[input.index],
					newNumberArray(load(sampleCount))
				)
				.emitBytecode(getBulk);
				getBulk.node.visitLabel(label());
			}
			//fill scratch arrays.
			for (Input input : this.gridInputs.values()) {
				invokeInstance(
					getField(
						load(self),
						input.fieldInfo(getBulk)
					),
					methodInfo,
					load(seed),
					load(x),
					load(y),
					load(scratches[input.index])
				)
				.emitBytecode(getBulk);
				getBulk.node.visitLabel(label());
			}
			//fill samples.
			getBulk.scopes.pushScope();
			LazyVarInfo index = getBulk.scopes.addVariable("index", TypeInfos.INT);
			for_(
				new LoopName(null),
				store(index, ldc(0)),
				lt(this, load(index), load(sampleCount)),
				inc(index, 1),
				numberArrayStore(
					load(samples),
					load(index),
					invokeStatic(
						new MethodInfo(
							ACC_PUBLIC | ACC_STATIC,
							getBulk.clazz.info,
							"evaluate",
							TypeInfos.DOUBLE,
							types('I', 'I', 'D', this.gridInputs.size())
						),
						Stream.concat(
							Stream.of(
								maybeAdd(this, x, index, 0, methodDimension),
								maybeAdd(this, y, index, 1, methodDimension)
							),
							this.gridInputs.values().stream().map((Input input) -> (
								numberArrayLoad(load(scratches[input.index]), load(index))
							))
						)
						.toArray(InsnTree[]::new)
					)
				)
			)
			.emitBytecode(getBulk);
			getBulk.scopes.popScope();
			//return.
			return_(noop).emitBytecode(getBulk);
			getBulk.endCode();
		}
	}
}