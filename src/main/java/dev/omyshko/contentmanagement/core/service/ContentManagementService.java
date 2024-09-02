package dev.omyshko.contentmanagement.core.service;

import dev.omyshko.contentmanagement.core.model.Project;

/**
 * <h6>Responsibilities </h6>
 * <p>Working with content allowing to CRUD the data hiding the details of Git or other sources interaction</p>
 * <h6>Domain </h6>
 * <p>Projects, Components</p>
 */
public class ContentManagementService {

    //private final ProjectsRepository projects;

    /**
     * Start working session.
     *
     * This is a question to technology.
     * Working with *git* requires you to pull the repo and do all the changes locally
     * But if some remote content doesn't need to be pulled before update?
     * Then we need to introduce some king of internal abstraction like *open* which depending on remote source will do a pull or nothing
     *
     * Work starts
     *  -> open //After this action all resources are synced and will be left in this state. Thing about it as a working session. You start working on a feature and first thing that you do it pull the changes. This is the same thing
     *      -> checkout a branch or whatever
     *
     *  -> Talk with AI and prepare the changes
     *
     *  -> push changes
     *      * Changes are pushed to separate branch
     *  -> close
     *
     * @param project
     */
    public void openProject(Project project) {

    }

}
