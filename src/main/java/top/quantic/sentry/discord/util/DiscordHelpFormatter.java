package top.quantic.sentry.discord.util;

import com.google.common.base.Splitter;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionDescriptor;

import java.util.Collection;
import java.util.Iterator;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class DiscordHelpFormatter extends BuiltinHelpFormatter {
    /**
     * Makes a formatter with a given overall row width and column separator width.
     *
     * @param desiredOverallWidth         how many characters wide to make the overall help display
     * @param desiredColumnSeparatorWidth how many characters wide to make the separation between option column and
     */
    public DiscordHelpFormatter(int desiredOverallWidth, int desiredColumnSeparatorWidth) {
        super(desiredOverallWidth, desiredColumnSeparatorWidth);
    }

    @Override
    protected void addHeaders(Collection<? extends OptionDescriptor> options) {
        addOptionRow("*Parameters with options*" + (hasRequiredOption(options) ? " (\\* = required)" : ""));
    }

    @Override
    protected void addOptions(Collection<? extends OptionDescriptor> options) {
        for (OptionDescriptor each : options) {
            if (!each.representsNonOptions()) {
                addOptionRow("**" + createOptionDisplay(each) + "**", createDescriptionDisplay(each));
            }
        }
    }

    @Override
    protected String optionLeader(String option) {
        return option.length() > 1 ? "" : "-";
    }

    @Override
    protected String createOptionDisplay(OptionDescriptor descriptor) {
        StringBuilder buffer = new StringBuilder(descriptor.isRequired() ? "\\* " : "");
        for (Iterator<String> i = descriptor.options().iterator(); i.hasNext(); ) {
            String option = i.next();
            buffer.append(optionLeader(option));
            buffer.append(option);
            if (i.hasNext()) {
                buffer.append(", ");
            }
        }
        maybeAppendOptionInfo(buffer, descriptor);
        return buffer.toString();
    }

    @Override
    protected void addNonOptionsDescription(Collection<? extends OptionDescriptor> options) {
        OptionDescriptor nonOptions = findAndRemoveNonOptionsSpec(options);
        if (shouldShowNonOptionArgumentDisplay(nonOptions)) {
            addNonOptionRow("*Parameters*");
            Splitter.on('\n')
                .splitToList(nonOptions.description())
                .forEach(this::addNonOptionRow);
        }
    }

    @Override
    protected void appendTypeIndicator(StringBuilder buffer, String typeIndicator, String description, char start, char end) {
        buffer.append(' ').append(start);
        if (!isBlank(description)) {
            buffer.append(description);
        }
        buffer.append(end);
    }
}
