package dev.omyshko.contentmanagement.api.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.omyshko.contentmanagement.api.KnowledgeBaseApiDelegate;
import dev.omyshko.contentmanagement.api.utils.RequestUtils;
import dev.omyshko.contentmanagement.knowledgebase.KnowledgeBaseService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Component
public class KnowledgeBaseEndpoint implements KnowledgeBaseApiDelegate {

    private final NativeWebRequest request;

    private final ObjectMapper objectMapper;

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * //Read everything in KB directory. Each directory has to have description
     * /*
     * * Є KB
     * * А є Інструкція як користуватись KB.
     * * Пошук за допомогою інструкції буде шукати по KB і повертати текст
     */
    @Override
    public ResponseEntity<String> search(String body) {
        String searchRequest = body;
        MediaType requestContentType = RequestUtils.getRequestContentType(request);

        //If request was made using application/json then get 'message' property
        if (MediaType.APPLICATION_JSON.includes(requestContentType)) {
            searchRequest = convertFromJson(body);
        }

        return ResponseEntity.ok(knowledgeBaseService.search(searchRequest));
    }

    @Override
    public ResponseEntity<String> getKnowledgeBaseInfo(String topicCodesCsv) {

        if (StringUtils.isNotBlank(topicCodesCsv)) {
            List<String> topicCodes = RequestUtils.parseCsv(topicCodesCsv);
            return ResponseEntity.ok(knowledgeBaseService.getTopicsInfo(topicCodes));
        }

        return ResponseEntity.ok(knowledgeBaseService.getTableOfContent());
    }

    private String convertFromJson(String body) {
        try {
            return objectMapper.readValue(body, SearchRequest.class).message();
        } catch (JsonProcessingException e) {
            return body;
        }
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    private record SearchRequest(String message) {
    }
}
