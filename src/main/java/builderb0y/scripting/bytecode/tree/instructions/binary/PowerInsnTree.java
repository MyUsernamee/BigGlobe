package builderb0y.scripting.bytecode.tree.instructions.binary;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import builderb0y.bigglobe.math.FastPow;
import builderb0y.scripting.bytecode.ExtendedOpcodes;
import builderb0y.scripting.bytecode.MethodCompileContext;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.TypeInfo.Sort;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.bytecode.tree.InvalidOperandException;
import builderb0y.scripting.parsing.ExpressionParser;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class PowerInsnTree extends BinaryInsnTree {

	public final PowMode mode;

	public PowerInsnTree(InsnTree left, InsnTree right, PowMode mode) {
		super(left, right, mode.opcode);
		this.mode = mode;
	}

	public static PowMode validate(TypeInfo left, TypeInfo right) {
		PowMode mode = switch (TypeInfos.widenToInt(right).getSort()) {
			case INT -> switch (TypeInfos.widenToInt(left).getSort()) {
				case INT -> PowMode.IIPOW;
				case LONG -> PowMode.LIPOW;
				case FLOAT -> PowMode.FIPOW;
				case DOUBLE -> PowMode.DIPOW;
				default -> null;
			};
			case FLOAT -> {
				if (left.isNumber()) {
					yield left.getSort() == Sort.DOUBLE ? PowMode.DDPOW : PowMode.FFPOW;
				}
				yield null;
			}
			case DOUBLE -> {
				if (left.isNumber()) {
					yield PowMode.DDPOW;
				}
				yield null;
			}
			default -> null;
		};
		if (mode != null) {
			return mode;
		}
		throw new InvalidOperandException("Can't pow " + left + " and " + right);
	}

	public static InsnTree create(ExpressionParser parser, InsnTree left, InsnTree right) {
		PowMode mode = validate(left.getTypeInfo(), right.getTypeInfo());
		ConstantValue leftConstant = left.getConstantValue();
		ConstantValue rightConstant = right.getConstantValue();
		if (leftConstant.isConstant() && rightConstant.isConstant()) {
			return switch (mode) {
				case IIPOW -> ldc(FastPow.pow(leftConstant.   asInt(), rightConstant.   asInt()));
				case LIPOW -> ldc(FastPow.pow(leftConstant.  asLong(), rightConstant.   asInt()));
				case FIPOW -> ldc(FastPow.pow(leftConstant. asFloat(), rightConstant.   asInt()));
				case DIPOW -> ldc(FastPow.pow(leftConstant.asDouble(), rightConstant.   asInt()));
				case FFPOW -> ldc(FastPow.pow(leftConstant. asFloat(), rightConstant. asFloat()));
				case DDPOW -> ldc(   Math.pow(leftConstant.asDouble(), rightConstant.asDouble()));
			};
		}
		left  = left .cast(parser, mode.leftType,  CastMode.EXPLICIT_THROW);
		right = right.cast(parser, mode.rightType, CastMode.EXPLICIT_THROW);
		return new PowerInsnTree(left, right, mode);
	}

	@Override
	public void emitBytecode(MethodCompileContext method) {
		ConstantValue rightConstant = this.right.getConstantValue();
		int power;
		if (rightConstant.isConstant() && (power = (int)(rightConstant.asDouble())) == rightConstant.asDouble()) {
			if (power == 2) { //special-handle x ^ 2.
				this.left.emitBytecode(method);
				method.node.visitInsn(this.left.getTypeInfo().isDoubleWidth() ? DUP2 : DUP);
				method.node.visitInsn(this.left.getTypeInfo().getOpcode(IMUL));
				return;
			}
			else { //use generated function for x ^ (any integer except 2).
				this.left.emitBytecode(method);
				method.node.visitInvokeDynamicInsn(
					"pow",
					switch (this.mode) {
						case IIPOW -> "(I)I";
						case LIPOW -> "(J)J";
						case FIPOW, FFPOW -> "(F)F";
						case DIPOW, DDPOW -> "(D)D";
					},
					new Handle(
						H_INVOKESTATIC,
						Type.getInternalName(FastPow.class),
						"getCallSite",
						"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;I)Ljava/lang/invoke/CallSite;",
						false
					),
					power
				);
				return;
			}
		}
		this.left.emitBytecode(method);
		this.right.emitBytecode(method);
		method.node.visitMethodInsn(
			INVOKESTATIC,
			Type.getInternalName(this.mode == PowMode.DDPOW ? Math.class : FastPow.class),
			"pow",
			switch (this.mode) {
				case IIPOW -> "(II)I";
				case LIPOW -> "(JI)J";
				case FIPOW -> "(FI)F";
				case DIPOW -> "(DI)D";
				case FFPOW -> "(FF)F";
				case DDPOW -> "(DD)D";
				default    -> throw new AssertionError(this.mode);
			},
			false
		);
	}

	public static enum PowMode {
		IIPOW(ExtendedOpcodes.IIPOW, TypeInfos.INT, TypeInfos.INT),
		LIPOW(ExtendedOpcodes.LIPOW, TypeInfos.LONG, TypeInfos.INT),
		FIPOW(ExtendedOpcodes.FIPOW, TypeInfos.FLOAT, TypeInfos.INT),
		DIPOW(ExtendedOpcodes.DIPOW, TypeInfos.DOUBLE, TypeInfos.INT),
		FFPOW(ExtendedOpcodes.FFPOW, TypeInfos.FLOAT, TypeInfos.FLOAT),
		DDPOW(ExtendedOpcodes.DDPOW, TypeInfos.DOUBLE, TypeInfos.DOUBLE);

		public final int opcode;
		public final TypeInfo leftType, rightType;

		PowMode(int opcode, TypeInfo leftType, TypeInfo rightType) {
			this.opcode = opcode;
			this.leftType = leftType;
			this.rightType = rightType;
		}
	}
}