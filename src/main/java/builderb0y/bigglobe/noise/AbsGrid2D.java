package builderb0y.bigglobe.noise;

public class AbsGrid2D extends AbsGrid implements UnaryGrid2D {

	public final Grid2D grid;

	public AbsGrid2D(Grid2D grid) {
		super(grid);
		this.grid = grid;
	}

	@Override
	public Grid2D getGrid() {
		return this.grid;
	}
}