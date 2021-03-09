package common.mixins.dimthread.modfixer;

import com.google.common.collect.Lists;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.mantle.config.Config;
import slimeknights.mantle.recipe.TagPreference;

import java.util.*;
import java.util.function.Supplier;

@Mixin(value = TagPreference.class, remap = false)
public class MantleMixin<T extends IForgeRegistryEntry<T>> {

    @Shadow @Final private Supplier<ITagCollection<T>> collection;
    private final Map<ResourceLocation, Optional<T>> cpreferenceCache = Collections.synchronizedMap(new HashMap());

    public Optional<T> getPreference(ITag<T> tag) {
        ResourceLocation tagName = ((ITagCollection)this.collection.get()).getValidatedIdFromTag(tag);
        return (Optional)this.cpreferenceCache.computeIfAbsent(tagName, (name) -> {
            if (tag instanceof Tags.IOptionalNamedTag && ((Tags.IOptionalNamedTag)tag).isDefaulted()) {
                return Optional.empty();
            } else {
                List<? extends T> elements = tag.getAllElements();
                if (elements.isEmpty()) {
                    return Optional.empty();
                } else if (elements.size() == 1) {
                    return Optional.of(elements.get(0));
                } else {
                    List<? extends T> sortedElements = Lists.newArrayList(elements);
                    sortedElements.sort(Comparator.comparingInt(this::getSortIndex));
                    return Optional.of(sortedElements.get(0));
                }
            }
        });
    }


    private int getSortIndex(IForgeRegistryEntry<?> entry) {
        List<String> entries = (List) Config.TAG_PREFERENCES.get();
        int index = entries.indexOf(((ResourceLocation)Objects.requireNonNull(entry.getRegistryName())).getNamespace());
        return index == -1 ? entries.size() : index;
    }
}
