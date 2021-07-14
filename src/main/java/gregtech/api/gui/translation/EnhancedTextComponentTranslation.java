package gregtech.api.gui.translation;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextComponentTranslationFormatException;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class EnhancedTextComponentTranslation extends TextComponentTranslation {

    public static final Pattern ENHANCED_STRING_VARIABLE_PATTERN = Pattern.compile("%(\\\\d+\\\\$)?([-#+ 0,(\\\\<]*)?(\\\\d+)?(\\\\.\\\\d+)?([tT])?([a-zA-Z%])");

    private final List<ITextComponent> copyChildren;

    public EnhancedTextComponentTranslation(final String translationKey, final Object... args) {
        super(translationKey, args);
        this.copyChildren = ObfuscationReflectionHelper.getPrivateValue(TextComponentTranslation.class, this, "children");
    }

    @Override
    protected void initializeFromFormat(final String format) {
        final Matcher matcher = ENHANCED_STRING_VARIABLE_PATTERN.matcher(format);
        int argIndex = 0;
        int start = 0;
        Object[] formatArgs = getFormatArgs();

        try {
            int end;

            for (; matcher.find(start); start = end) {
                final int found = matcher.start();
                end = matcher.end();

                // Text before the first %
                if (found > start) {
                    final TextComponentString text = new TextComponentString(String.format(format.substring(start, found)));
                    text.getStyle().setParentStyle(this.getStyle());
                    this.copyChildren.add(text);
                }

                final String formatOption = matcher.group(6);
                final String string = format.substring(found, end);

                // Escape sequence %%
                if ("%".equals(formatOption) && "%%".equals(string)) {
                    final TextComponentString text = new TextComponentString("%");
                    text.getStyle().setParentStyle(this.getStyle());
                    this.copyChildren.add(text);
                } else {
                    // Argument number specifier
                    final String arg = matcher.group(1);
                    final int index = arg != null ? Integer.parseInt(arg) - 1 : argIndex++;

                    if (index < formatArgs.length) {
                        final Object object = formatArgs[index];
                        final ITextComponent text;
                        if (object == null) {
                            text = new TextComponentString("null");
                        }
                        else if (object instanceof ITextComponent) {
                            text = (ITextComponent) object;
                        } else {
                            // Concatenate all the format options except the index specifier
                            final StringBuilder builder = new StringBuilder();
                            builder.append('%');
                            // groupCount doesn't include group 0 hence <=
                            for (int g = 2; g <= matcher.groupCount(); ++g) {
                                final String group = matcher.group(g);
                                if (group != null) {
                                    builder.append(group);
                                }
                            }
                            text = new TextComponentString(String.format(builder.toString(), fixArg(formatOption, object)));
                        }
                        text.getStyle().setParentStyle(this.getStyle());
                        this.copyChildren.add(text);
                    }
                }
            }

            // Trailing text
            if (start < format.length()) {
                final TextComponentString text = new TextComponentString(String.format(format.substring(start)));
                text.getStyle().setParentStyle(this.getStyle());
                this.copyChildren.add(text);
            }
        } catch (IllegalFormatException | NumberFormatException e) {
            throw new TextComponentTranslationFormatException(this, e);
        }
    }

    private static Object fixArg(final String formatOption, final Object original) {
        if (original instanceof String) {
            final String string = (String) original;
            // Short circuit common case
            if ("s".equals(formatOption)) {
                return string;
            }
            if ("d".equals(formatOption) || "o".equals(formatOption) || "x".equalsIgnoreCase(formatOption)) {
                return Long.valueOf(string);
            }
            if ("e".equalsIgnoreCase(formatOption) || "f".equals(formatOption) || "g".equalsIgnoreCase(formatOption) || "a".equalsIgnoreCase(formatOption)) {
                return Double.valueOf(string);
            }
        }
        return original;
    }

    @Override
    public EnhancedTextComponentTranslation createCopy() {
        final Object[] formatArgs = getFormatArgs();
        final Object[] copyArgs = new Object[formatArgs.length];

        for (int i = 0; i < formatArgs.length; ++i) {
            if (formatArgs[i] instanceof ITextComponent) {
                copyArgs[i] = ((ITextComponent) formatArgs[i]).createCopy();
            } else {
                copyArgs[i] = formatArgs[i];
            }
        }

        final EnhancedTextComponentTranslation copy = new EnhancedTextComponentTranslation(getKey(), copyArgs);
        copy.setStyle(this.getStyle().createShallowCopy());

        for (ITextComponent sibling : this.getSiblings()) {
            copy.appendSibling(sibling.createCopy());
        }
        return copy;
    }
}
