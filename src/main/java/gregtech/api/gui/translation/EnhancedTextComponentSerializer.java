package gregtech.api.gui.translation;

import java.lang.reflect.Type;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;

public class EnhancedTextComponentSerializer extends ITextComponent.Serializer {

    private static final Gson GSON;

    @Override
    public ITextComponent deserialize(final JsonElement element, final Type type, final JsonDeserializationContext context) throws JsonParseException {
        final ITextComponent superResult = super.deserialize(element, type, context);
        if (!(superResult instanceof TextComponentTranslation)) {
            return superResult;
        }
        final TextComponentTranslation original = (TextComponentTranslation) superResult;
        final EnhancedTextComponentTranslation result = new EnhancedTextComponentTranslation(original.getKey(), original.getFormatArgs());
        for (ITextComponent sibling : original.getSiblings()) {
            result.appendSibling(sibling);
        }
        return result;
    }

    /**
     * Serializes a component into JSON.
     */
    public static String componentToJson(final ITextComponent component) {
        return GSON.toJson(component);
    }

    /**
     * Parses a JSON string into a {@link ITextComponent}, with strict parsing.
     * 
     * @see #fromJsonLenient(String)
     * @see {@link com.google.gson.stream.JsonReader#setLenient(boolean)}
     */
    @Nullable
    public static ITextComponent jsonToComponent(final String json) {
        return JsonUtils.gsonDeserialize(GSON, json, ITextComponent.class, false);
    }

    /**
     * Parses a JSON string into a {@link ITextComponent}, being lenient upon parse
     * errors.
     * 
     * @see #jsonToComponent(String)
     * @see {@link com.google.gson.stream.JsonReader#setLenient(boolean)}
     */
    @Nullable
    public static ITextComponent fromJsonLenient(final String json) {
        return JsonUtils.gsonDeserialize(GSON, json, ITextComponent.class, true);
    }

    static {
        GsonBuilder gsonbuilder = new GsonBuilder();
        gsonbuilder.registerTypeHierarchyAdapter(ITextComponent.class, new EnhancedTextComponentSerializer());
        gsonbuilder.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
        gsonbuilder.registerTypeAdapterFactory(new EnumTypeAdapterFactory());
        GSON = gsonbuilder.create();
    }
}
