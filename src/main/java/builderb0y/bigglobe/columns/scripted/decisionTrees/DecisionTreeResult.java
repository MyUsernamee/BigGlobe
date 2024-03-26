package builderb0y.bigglobe.columns.scripted.decisionTrees;

import org.jetbrains.annotations.Nullable;

import builderb0y.autocodec.annotations.MemberUsage;
import builderb0y.autocodec.annotations.UseCoder;
import builderb0y.bigglobe.BigGlobeMod;
import builderb0y.bigglobe.codecs.CoderRegistry;
import builderb0y.bigglobe.codecs.CoderRegistryTyped;
import builderb0y.bigglobe.columns.scripted.AccessSchema;
import builderb0y.bigglobe.columns.scripted.dependencies.ColumnValueDependencyHolder;
import builderb0y.bigglobe.columns.scripted.compile.DataCompileContext;
import builderb0y.scripting.bytecode.tree.InsnTree;

@UseCoder(name = "REGISTRY", in = DecisionTreeResult.class, usage = MemberUsage.FIELD_CONTAINS_HANDLER)
public interface DecisionTreeResult extends CoderRegistryTyped<DecisionTreeResult>, ColumnValueDependencyHolder {

	public static final CoderRegistry<DecisionTreeResult> REGISTRY = new CoderRegistry<>(BigGlobeMod.modID("decision_tree_result"));
	public static final Object INITIALIZER = new Object() {{
		REGISTRY.registerAuto(BigGlobeMod.modID("constant"), ConstantDecisionTreeResult.class);
	}};

	public abstract InsnTree createResult(DataCompileContext context, AccessSchema accessSchema, @Nullable InsnTree loadY);
}