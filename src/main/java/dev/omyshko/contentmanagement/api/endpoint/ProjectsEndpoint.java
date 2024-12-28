package dev.omyshko.contentmanagement.api.endpoint;

import dev.omyshko.contentmanagement.api.ProjectsApiDelegate;
import dev.omyshko.contentmanagement.api.model.ProcessProjectRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Optional;

@AllArgsConstructor
public class ProjectsEndpoint implements ProjectsApiDelegate {

    private final NativeWebRequest request;

    @Override
    public ResponseEntity<String> processProject(ProcessProjectRequest processProjectRequest) {
        return ProjectsApiDelegate.super.processProject(processProjectRequest);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }
}
