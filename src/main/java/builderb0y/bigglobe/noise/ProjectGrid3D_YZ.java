package builderb0y.bigglobe.noise;

import java.util.Arrays;

import builderb0y.autocodec.annotations.UseName;

public class ProjectGrid3D_YZ implements Grid3D {

	public final @UseName("2D_grid") Grid2D grid;

	public ProjectGrid3D_YZ(Grid2D grid) {
		this.grid = grid;
	}

	@Override
	public double minValue() {
		return this.grid.minValue();
	}

	@Override
	public double maxValue() {
		return this.grid.maxValue();
	}

	@Override
	public double getValue(long seed, int x, int y, int z) {
		return this.grid.getValue(seed, y, z);
	}

	@Override
	public void getBulkX(long seed, int startX, int y, int z, double[] samples, int sampleCount) {
		Arrays.fill(samples, 0, sampleCount, this.grid.getValue(seed, y, z));
	}

	@Override
	public void getBulkY(long seed, int x, int startY, int z, double[] samples, int sampleCount) {
		this.grid.getBulkX(seed, startY, z, samples, sampleCount);
	}

	@Override
	public void getBulkZ(long seed, int x, int y, int startZ, double[] samples, int sampleCount) {
		this.grid.getBulkY(seed, y, startZ, samples, sampleCount);
	}
}