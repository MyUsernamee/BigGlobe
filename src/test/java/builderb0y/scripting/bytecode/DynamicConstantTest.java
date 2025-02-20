package builderb0y.scripting.bytecode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.junit.jupiter.api.Test;

import builderb0y.scripting.util.TypeInfos;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public class DynamicConstantTest {

	public static int constant;

	@Test
	public void test() throws Throwable {
		this.test(1);
		this.test(2);
	}

	public void test(int implNumber) throws Throwable {
		System.out.println("test " + implNumber);
		TypeInfo superClass = TypeInfo.of(C.class);
		ClassCompileContext clazz = new ClassCompileContext(
			ACC_PUBLIC,
			ClassType.CLASS,
			superClass.getInternalName() + "$Impl$" + implNumber,
			superClass,
			TypeInfo.ARRAY_FACTORY.empty()
		);

		clazz.node.visit(V17, ACC_PUBLIC, clazz.info.getInternalName(), null, superClass.getInternalName(), null);
		clazz.addNoArgConstructor(ACC_PUBLIC);

		MethodCompileContext getConstant = clazz.newMethod(ACC_PUBLIC, "getConstant", TypeInfos.INT);
		return_(
			ldc(
				MethodInfo.getMethod(DynamicConstantTest.class, "getConstant")
			)
		)
		.emitBytecode(getConstant);
		getConstant.endCode();

		System.out.println("Defining...");
		MethodHandles.Lookup lookup = MethodHandles.lookup().defineHiddenClass(clazz.toByteArray(), true);
		System.out.println("Constructing...");
		C implementation = (C)(lookup.findConstructor(lookup.lookupClass(), MethodType.methodType(void.class)).invoke());
		System.out.println("Invoking...");
		System.out.println(implementation.getConstant());
		System.out.println(implementation.getConstant());
	}

	public static int getConstant(MethodHandles.Lookup lookup, String name, Object type) {
		System.out.println("Called DynamicConstantTest.getConstant(): lookup: " + lookup + ", name: " + name + ", type: " + type);
		return constant++;
	}

	public static abstract class C {

		public abstract int getConstant();
	}
}