package builderb0y.bigglobe.trees.decoration;

import builderb0y.bigglobe.trees.TreeGenerator;
import builderb0y.bigglobe.trees.trunks.TrunkConfig;

public interface TrunkLayerDecorator {

	public abstract void decorate(TreeGenerator generator, TrunkConfig trunk, int y);
}