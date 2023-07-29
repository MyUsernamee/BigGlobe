package builderb0y.scripting.bytecode.tree.instructions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.util.ArrayFactories;
import builderb0y.scripting.bytecode.MethodInfo;
import builderb0y.scripting.bytecode.TypeInfo;
import builderb0y.scripting.bytecode.tree.ConstantValue;
import builderb0y.scripting.bytecode.tree.InsnTree;
import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class InvokeStaticInsnTree extends InvokeBaseInsnTree {

	public InvokeStaticInsnTree(MethodInfo method, InsnTree... args) {
		super(method, args);
		checkArguments(this.method.paramTypes, this.args);
	}

	public InvokeStaticInsnTree(MethodInfo method, InsnTree[] args, boolean dummy) {
		super(method, args);
	}

	public static InsnTree create(MethodInfo method, InsnTree... args) {
		if (!method.isStatic()) {
			throw new IllegalArgumentException("Non-static method: " + method);
		}
		checkArguments(method.paramTypes, args);
		notPure:
		if (method.isPure() && isPrimitive(method.returnType)) {
			Object[] constantArgs = getConstantArgs(args);
			if (constantArgs == null) break notPure;
			try {
				MethodHandle handle = getMethodHandle(method);
				return ldc(
					handle.invokeWithArguments(constantArgs),
					method.returnType
				);
			}
			catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
		return new InvokeStaticInsnTree(method, args, true);
	}

	public static boolean isPrimitive(TypeInfo type) {
		return type.isPrimitiveValue() || type.equals(TypeInfos.STRING);
	}

	public static Object @Nullable [] getConstantArgs(InsnTree... args) {
		int argCount = args.length;
		Object[] constantArgs = new Object[argCount];
		for (int index = 0; index < argCount; index++) {
			ConstantValue constant = args[index].getConstantValue();
			if (constant.isConstant()) {
				constantArgs[index] = constant.asJavaObject();
			}
			else {
				return null;
			}
		}
		return constantArgs;
	}

	public static MethodHandle getMethodHandle(MethodInfo method) throws NoSuchMethodException, IllegalAccessException {
		Class<?> owner = method.owner.toClass();
		MethodType methodType = getMethodType(method);
		return MethodHandles.lookup().findStatic(owner, method.name, methodType);
	}

	public static MethodType getMethodType(MethodInfo method) {
		return MethodType.methodType(
			method.returnType.toClass(),
			Arrays.stream(method.paramTypes)
			.map(TypeInfo::toClass)
			.toArray(ArrayFactories.CLASS)
		);
	}
}