package builderb0y.bigglobe.scripting;

import java.util.random.RandomGenerator;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import org.jetbrains.annotations.Nullable;

import builderb0y.bigglobe.noise.Permuter;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InsnTree.CastMode;
import builderb0y.scripting.bytecode.tree.conditions.ConditionTree;
import builderb0y.scripting.environments.MutableScriptEnvironment.MethodHandler;
import builderb0y.scripting.environments.ScriptEnvironment;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.parsing.ExpressionReader.CursorPos;
import builderb0y.scripting.parsing.ScriptParsingException;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class RandomScriptEnvironment implements ScriptEnvironment {

	public static final TypeInfo
		RANDOM_GENERATOR_TYPE  = type(RandomGenerator.class);
	public static final MethodInfo
		CONSTRUCTOR            = MethodInfo.findConstructor(  Permuter.class, long.class),
		PERMUTE_INT            = MethodInfo.findMethod(       Permuter.class, "permute", long.class, int.class),
		NEXT_BOOLEAN           = MethodInfo.findMethod(RandomGenerator.class, "nextBoolean"),
		NEXT_CHANCED_BOOLEAN_F = MethodInfo.findMethod(       Permuter.class, "nextChancedBoolean", RandomGenerator.class, float.class),
		NEXT_CHANCED_BOOLEAN_D = MethodInfo.findMethod(       Permuter.class, "nextChancedBoolean", RandomGenerator.class, double.class),
		NEXT_INT_0             = MethodInfo.findMethod(RandomGenerator.class, "nextInt"),
		NEXT_INT_1             = MethodInfo.findMethod(RandomGenerator.class, "nextInt", int.class),
		NEXT_INT_2             = MethodInfo.findMethod(RandomGenerator.class, "nextInt", int.class, int.class),
		NEXT_LONG_0            = MethodInfo.findMethod(RandomGenerator.class, "nextLong"),
		NEXT_LONG_1            = MethodInfo.findMethod(RandomGenerator.class, "nextLong", long.class),
		NEXT_LONG_2            = MethodInfo.findMethod(RandomGenerator.class, "nextLong", long.class, long.class),
		NEXT_FLOAT_0           = MethodInfo.findMethod(RandomGenerator.class, "nextFloat"),
		NEXT_FLOAT_1           = MethodInfo.findMethod(RandomGenerator.class, "nextFloat", float.class),
		NEXT_FLOAT_2           = MethodInfo.findMethod(RandomGenerator.class, "nextFloat", float.class, float.class),
		NEXT_DOUBLE_0          = MethodInfo.findMethod(RandomGenerator.class, "nextDouble"),
		NEXT_DOUBLE_1          = MethodInfo.findMethod(RandomGenerator.class, "nextDouble", double.class),
		NEXT_DOUBLE_2          = MethodInfo.findMethod(RandomGenerator.class, "nextDouble", double.class, double.class),
		ROUND_INT_F            = MethodInfo.findMethod(       Permuter.class, "roundRandomlyI", RandomGenerator.class, float.class),
		ROUND_INT_D            = MethodInfo.findMethod(       Permuter.class, "roundRandomlyI", RandomGenerator.class, double.class),
		ROUND_LONG_F           = MethodInfo.findMethod(       Permuter.class, "roundRandomlyL", RandomGenerator.class, float.class),
		ROUND_LONG_D           = MethodInfo.findMethod(       Permuter.class, "roundRandomlyL", RandomGenerator.class, double.class);

	public final InsnTree loader;

	public RandomScriptEnvironment(InsnTree loader) {
		this.loader = loader;
	}

	@Override
	public @Nullable InsnTree getVariable(ExpressionParser parser, String name) throws ScriptParsingException {
		return name.equals("random") ? this.loader : null;
	}

	@Override
	public @Nullable TypeInfo getType(ExpressionParser parser, String name) throws ScriptParsingException {
		return name.equals("Random") ? RANDOM_GENERATOR_TYPE : null;
	}

	@Override
	public @Nullable InsnTree getMethod(ExpressionParser parser, InsnTree receiver, String name, InsnTree... arguments) throws ScriptParsingException {
		ConstantValue constant = receiver.getConstantValue();
		if (constant.isConstant() && constant.asJavaObject().equals(RANDOM_GENERATOR_TYPE) && name.equals("new")) {
			if (arguments.length == 0) throw new ScriptParsingException("Random.new() requires a seed.", parser.input);
			InsnTree seed = arguments[0].cast(parser, TypeInfos.LONG, CastMode.IMPLICIT_THROW);
			for (int index = 1, length = arguments.length; index < length; index++) {
				InsnTree next = arguments[index].cast(parser, TypeInfos.INT, CastMode.IMPLICIT_THROW);
				seed = invokeStatic(PERMUTE_INT, seed, next);
			}
			return newInstance(CONSTRUCTOR, seed);
		}
		if (!receiver.getTypeInfo().extendsOrImplements(RANDOM_GENERATOR_TYPE)) return null;

		return switch (name) {
			case "nextBoolean" -> switch (arguments.length) {
				case 0 -> invokeInterface(receiver, NEXT_BOOLEAN, arguments);
				case 1 -> switch (arguments[0].getTypeInfo().getSort()) {
					case FLOAT  -> invokeStatic(NEXT_CHANCED_BOOLEAN_F, receiver, arguments[0]);
					case DOUBLE -> invokeStatic(NEXT_CHANCED_BOOLEAN_D, receiver, arguments[0]);
					default -> throw new ScriptParsingException("nextBoolean() requires either 0 arguments, or 1 float or double argument", parser.input);
				};
				default -> throw new ScriptParsingException("nextBoolean() requires either 0 arguments, or 1 float or double argument, got " + arguments.length, parser.input);
			};
			case "nextInt" -> switch (arguments.length) {
				case 0 -> MethodHandler.make(parser, receiver, name, NEXT_INT_0, arguments);
				case 1 -> MethodHandler.make(parser, receiver, name, NEXT_INT_1, arguments);
				case 2 -> MethodHandler.make(parser, receiver, name, NEXT_INT_2, arguments);
				default -> throw new ScriptParsingException("nextInt() requires 0, 1, or 2 arguments, got " + arguments.length, parser.input);
			};
			case "nextLong" -> switch (arguments.length) {
				case 0 -> MethodHandler.make(parser, receiver, name, NEXT_LONG_0, arguments);
				case 1 -> MethodHandler.make(parser, receiver, name, NEXT_LONG_1, arguments);
				case 2 -> MethodHandler.make(parser, receiver, name, NEXT_LONG_2, arguments);
				default -> throw new ScriptParsingException("nextLong() requires 0, 1, or 2 arguments, got " + arguments.length, parser.input);
			};
			case "nextFloat" -> switch (arguments.length) {
				case 0 -> MethodHandler.make(parser, receiver, name, NEXT_FLOAT_0, arguments);
				case 1 -> MethodHandler.make(parser, receiver, name, NEXT_FLOAT_1, arguments);
				case 2 -> MethodHandler.make(parser, receiver, name, NEXT_FLOAT_2, arguments);
				default -> throw new ScriptParsingException("nextFloat() requires 0, 1, or 2 arguments, got " + arguments.length, parser.input);
			};
			case "nextDouble" -> switch (arguments.length) {
				case 0 -> MethodHandler.make(parser, receiver, name, NEXT_DOUBLE_0, arguments);
				case 1 -> MethodHandler.make(parser, receiver, name, NEXT_DOUBLE_1, arguments);
				case 2 -> MethodHandler.make(parser, receiver, name, NEXT_DOUBLE_2, arguments);
				default -> throw new ScriptParsingException("nextDouble() requires 0, 1, or 2 arguments, got " + arguments.length, parser.input);
			};
			case "switch" -> {
				if (arguments.length < 2) {
					throw new ScriptParsingException("switch() requires at least 2 arguments", parser.input);
				}
				Int2ObjectSortedMap<InsnTree> cases = new Int2ObjectAVLTreeMap<>();
				for (int index = 0, length = arguments.length; index < length; index++) {
					cases.put(index, arguments[index]);
				}
				cases.defaultReturnValue(
					throw_(
						newInstance(
							constructor(
								ACC_PUBLIC,
								AssertionError.class,
								String.class
							),
							ldc("Random returned value out of range")
						)
					)
				);
				yield switch_(
					parser,
					invokeInterface(
						this.loader,
						NEXT_INT_1,
						ldc(arguments.length)
					),
					cases
				);
			}
			case "roundInt" -> {
				if (arguments.length == 1) {
					yield switch (arguments[0].getTypeInfo().getSort()) {
						case FLOAT  -> invokeStatic(ROUND_INT_F, receiver, arguments[0]);
						case DOUBLE -> invokeStatic(ROUND_INT_D, receiver, arguments[0]);
						default -> throw new ScriptParsingException("roundInt() requires an argument of type float or double", parser.input);
					};
				}
				else {
					throw new ScriptParsingException("roundInt() requires exactly 1 argument", parser.input);
				}
			}
			case "roundLong" -> {
				if (arguments.length == 1) {
					yield switch (arguments[0].getTypeInfo().getSort()) {
						case FLOAT  -> invokeStatic(ROUND_LONG_F, receiver, arguments[0]);
						case DOUBLE -> invokeStatic(ROUND_LONG_D, receiver, arguments[0]);
						default -> throw new ScriptParsingException("roundLong() requires an argument of type float or double", parser.input);
					};
				}
				else {
					throw new ScriptParsingException("roundLong() requires exactly 1 argument", parser.input);
				}
			}
			default -> null;
		};
	}

	@Override
	public @Nullable InsnTree parseMemberKeyword(ExpressionParser parser, InsnTree receiver, String name) throws ScriptParsingException {
		if (!receiver.getTypeInfo().extendsOrImplements(RANDOM_GENERATOR_TYPE)) return null;

		return switch (name) {
			case "if" -> randomIf(parser, receiver, false);
			case "unless" -> randomIf(parser, receiver, true);
			default -> null;
		};
	}

	public static InsnTree randomIf(ExpressionParser parser, InsnTree receiver, boolean negate) throws ScriptParsingException {
		parser.input.expectAfterWhitespace('(');
		InsnTree conditionInsnTree, body;
		InsnTree firstPart = parser.nextScript();
		CursorPos revert = parser.input.getCursor();
		if (parser.input.hasOperatorAfterWhitespace(":")) { //random.if (a: b)
			Sort sort = firstPart.getTypeInfo().getSort();
			if (sort != Sort.FLOAT && sort != Sort.DOUBLE) {
				parser.input.setCursor(revert);
				throw new ScriptParsingException("random.if() chance should be float or double, but was " + firstPart.getTypeInfo(), parser.input);
			}
			InsnTree secondPart = parser.nextScript();
			conditionInsnTree = invokeStatic(
				switch (sort) {
					case FLOAT -> NEXT_CHANCED_BOOLEAN_F;
					case DOUBLE -> NEXT_CHANCED_BOOLEAN_D;
					default -> throw new AssertionError(sort);
				},
				receiver,
				firstPart
			);
			body = secondPart;
		}
		else { //random.if (a)
			conditionInsnTree = invokeInterface(receiver, NEXT_BOOLEAN);
			body = firstPart;
		}
		parser.input.expectAfterWhitespace(')');
		ConditionTree conditionTree = condition(parser, conditionInsnTree);
		if (negate) conditionTree = not(conditionTree);
		if (parser.input.hasIdentifierAfterWhitespace("else")) {
			return ifElse(parser, conditionTree, body, parser.nextExpression());
		}
		else {
			return ifThen(parser, conditionTree, body);
		}
	}
}