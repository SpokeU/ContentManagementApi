package dev.omyshko.contentmanagement.instructions.changelog;

import dev.omyshko.contentmanagement.core.model.Component;
import dev.omyshko.contentmanagement.core.model.Project;
import dev.omyshko.contentmanagement.core.service.GitContentManager;
import dev.omyshko.contentmanagement.core.service.ProjectsRepository;
import dev.omyshko.contentmanagement.instructions.ResponseProcessor;
import dev.omyshko.contentmanagement.instructions.changelog.model.ChangeLog;
import dev.omyshko.contentmanagement.instructions.changelog.model.Operation;
import dev.omyshko.contentmanagement.instructions.model.RESPONSE_FORMAT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Responsible for knowing how to handle ChangeLog instructions returned by LLM.
 * - Parsing changelog
 * - Executing operations by delegating to appropriate ContentManager(Git, Confluence API etc.) according to project config
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class ChangeLogProcessor implements ResponseProcessor {

    private final ChangeLogParser changeLogParser;

    private final ProjectsRepository projects;

    private final GitContentManager gitService;

    @Override
    public ProcessingResult process(String text) {
        ChangeLog changeLog = changeLogParser.extract(text);
        //TODO Find out type of processor
        //component.getLocation() check type
        ProcessingResult processingResult = gitService.processChangeLog(changeLog);

        return new ProcessingResult(processingResult.toString());
    }

    private ProcessingResult processCreate(Project project, Component component, String changeSummary, String branch, Operation instruction) {
        /*TODO
           Content management system(abstract)
           Content management system interface. Open, CRUD, Close.
           *GitContentManagement
           *Confluence
           etc..

           So far straightforward git
           */

        gitService.addFileV2(project, component, branch, changeSummary, instruction.resource, instruction.content);
        return new ProcessingResult("Vary nice");
    }

    /*private ProcessingResult processCreate(String path, String filename, String content) {
        String result = "\nCreating file: " + filename + " \nContent: " + content;
        log.info(result);

        try {
            Path filePath = Paths.get(path, filename);

            Path parentDir = filePath.getParent();

            if (parentDir != null) {
                Files.createDirectories(parentDir);  // Creates directories if they don't exist
            }

            Files.write(filePath, content.getBytes());
        } catch (IOException e) {
            throw new ApiException("Error during file creation. " + path + filename, HttpStatus.BAD_REQUEST);
        }

        return new ProcessingResult(result);
    }*/

    @Override
    public RESPONSE_FORMAT getProcessedType() {
        return RESPONSE_FORMAT.CHANGE_LOG;
    }

}
