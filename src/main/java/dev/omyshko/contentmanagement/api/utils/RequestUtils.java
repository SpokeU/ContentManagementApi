package dev.omyshko.contentmanagement.api.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Arrays;
import java.util.List;

public class RequestUtils {

    public static MediaType getRequestContentType(NativeWebRequest request) {
        String contentType = request.getHeader("Content-Type");
        MediaType mediaType = MediaType.parseMediaType(contentType);
        return mediaType;
    }

    public static List<String> parseCsv(String s){
        return Arrays.asList(StringUtils.split(s, ','));
    }
}
