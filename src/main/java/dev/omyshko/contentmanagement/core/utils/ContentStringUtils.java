package dev.omyshko.contentmanagement.core.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ContentStringUtils {

    /**
     * Wraps 'content' with a tag. This is done primarily for LLM to separate content when merging few topics
     * String s = wrap("somecontent", "topic");
     *
     * @param content
     * @param tag
     * @return
     */
    public static String wrap(String content, String tag) {
        return "<" + tag + ">\n\n".concat(content).concat("\n\n</" + tag + ">");
    }

    /**
     * Convert the input string to lowercase, trim spaces, and replace spaces with dashes
     * <pre>
     * Content management api = content-management-api
     * </pre>
     */
    public static String toDashCase(String input) {
        return StringUtils.join(StringUtils.split(input.toLowerCase()), '-');
    }

}
