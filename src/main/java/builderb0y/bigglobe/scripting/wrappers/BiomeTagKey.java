package builderb0y.bigglobe.scripting.wrappers;

import java.lang.invoke.MethodHandles;
import java.util.random.RandomGenerator;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import builderb0y.bigglobe.versions.IdentifierVersions;
import builderb0y.scripting.bytecode.ConstantFactory;
import builderb0y.bigglobe.versions.RegistryKeyVersions;
import builderb0y.scripting.bytecode.TypeInfo;

import static builderb0y.scripting.bytecode.InsnTrees.*;

public record BiomeTagKey(TagKey<Biome> key) implements TagWrapper<Biome, BiomeEntry> {

	public static final TypeInfo TYPE = type(BiomeTagKey.class);
	public static final ConstantFactory CONSTANT_FACTORY = ConstantFactory.autoOfString();

	public static BiomeTagKey of(MethodHandles.Lookup caller, String name, Class<?> type, String id) {
		return of(id);
	}

	public static BiomeTagKey of(String id) {
		if (id == null) return null;
		return new BiomeTagKey(TagKey.of(RegistryKeyVersions.biome(), IdentifierVersions.create(id)));
	}

	@Override
	public BiomeEntry wrap(RegistryEntry<Biome> entry) {
		return new BiomeEntry(entry);
	}

	@Override
	public BiomeEntry random(RandomGenerator random) {
		return this.randomImpl(random);
	}

	@Override
	public BiomeEntry random(long seed) {
		return this.randomImpl(seed);
	}
}