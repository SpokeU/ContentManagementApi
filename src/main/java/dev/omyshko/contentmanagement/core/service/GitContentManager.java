package dev.omyshko.contentmanagement.core.service;

import dev.omyshko.contentmanagement.core.model.Component;
import dev.omyshko.contentmanagement.core.model.Project;
import dev.omyshko.contentmanagement.instructions.ResponseProcessor;
import dev.omyshko.contentmanagement.instructions.changelog.model.ChangeLog;
import dev.omyshko.contentmanagement.instructions.changelog.model.Operation;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.eclipse.jgit.lib.FileMode.REGULAR_FILE;

@Slf4j
@Service
public class GitContentManager {

    String username = "SpokeU";

    @Value("${GIT_API_KEY}")
    String password;

    private final String storagePath;
    private final ProjectsRepository projectsRepo;

    public GitContentManager(@Value("${app.storage.path:}") String storagePath, ProjectsRepository projects) {
        this.storagePath = storagePath;
        this.projectsRepo = projects;
    }

    //TODO Do initialization of git repos async
    @PostConstruct
    public void init() {
        List<Project> availableProjects = projectsRepo.findAll();

        for (Project project : availableProjects) {
            //syncProject(project); //TODO enable when needed
            //TODO error handling
        }

    }

    public void syncProject(Project project) {
        List<Component> components = project.getComponents();
        for (Component component : components) {
            Path componentPath = getRepoPath(project, component);
            try {
                Files.createDirectories(componentPath);


                String gitRepoUrl = component.getLocation();//TODO Check if its GIT component
                Repository repository = openExistingRepo(componentPath.toFile());

                if (repository != null) {
                    log.info("Existing repository found at: " + repository + ". Pulling...");
                    Git git = new Git(repository);
                    git.pull().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
                    log.info("Pull complete.");
                } else {
                    log.info("No existing repository found. Cloning...");
                    try (Git git = Git.cloneRepository()
                            .setURI(gitRepoUrl)
                            .setDirectory(componentPath.toFile())
                            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                            .call()) {
                        log.info("Repository cloned!");
                    }

                }

            } catch (Exception e) {
                String message = "Error while syncing project:" + project + " component:" + component;
                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
    }

    public ResponseProcessor.ProcessingResult processChangeLog(ChangeLog changeLog) {
        //1 Prepare stuff
        Project project = projectsRepo.find(changeLog.getHeader().getProject());
        Component component = projectsRepo.find(changeLog.getHeader().getProject(), changeLog.getHeader().getComponent());
        String mainBranch = "master";
        String targetBranch = changeLog.getHeader().getBranch();

        Path repoDir = getRepoPath(project, component);

        //2 Checkout branch
        try (Git git = Git.open(repoDir.toFile())) {
            git.checkout()
                    .setCreateBranch(true)
                    .setName(targetBranch)
                    .call();

            //Process file operations
            for (Operation operation : changeLog.getOperations()) {
                String filePath = operation.getResource();
                String content = operation.getContent();

                Path absoluteFilePath = Paths.get(repoDir.toString(), filePath);

                Path parentDir = absoluteFilePath.getParent();

                if (parentDir != null) {
                    Files.createDirectories(parentDir);  // Creates directories if they don't exist
                }

                Files.write(absoluteFilePath, content.getBytes());

                //Git add
                git.add().addFilepattern(filePath).call();
            }

            git.commit()
                    .setMessage(changeLog.getHeader().getChangeSummary())
                    .call();

            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                    .add(targetBranch)
                    .call();

            git.checkout()
                    .setName(mainBranch)
                    .call();
        } catch (IOException | GitAPIException e) {
            log.error("Shit happened processing change log: " + changeLog, e);
            e.printStackTrace();
        }

        //return to master branch


        return new ResponseProcessor.ProcessingResult("Ok very nice");//TODO form proper message
    }


    private static Repository openExistingRepo(File repoDir) {
        File gitDir = new File(repoDir, ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            // Return null if .git directory doesn't exist
            System.out.println("No .git directory found in " + repoDir.getAbsolutePath());
            return null;
        }

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            return builder.setGitDir(new File(repoDir, ".git"))
                    .readEnvironment() // scan environment GIT_* variables
                    .findGitDir() // scan up the file system tree for the .git directory
                    .build();
        } catch (IOException e) {
            // If there's no repository or an error occurs, we return null
            return null;
        }
    }

    //--------------
    /*
    Shit below is how things supposed to be.
    We want to manipulate based on .git internal structure of objects and trees instead of doing checkouts.
     */
    //--------------

    public void addFileV2(Project project, Component component, String targetBranch, String changeSummary, String filePath, String content) {
        String sourceBranch = "master";
        targetBranch = "feature/12";
        Path repoDir = getRepoPath(project, component);

        try {

            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(new File(repoDir.toFile(), ".git"))
                    .build();

            try (Git git = new Git(repository)) {
                // Create a new branch
                Ref newBranch = git.branchCreate()
                        .setName(targetBranch)
                        .call();

                // Get the parent commit (latest commit on the current branch)
                ObjectId headId = repository.resolve("HEAD");
                RevWalk revWalk = new RevWalk(repository);
                RevCommit parentCommit = revWalk.parseCommit(headId);
                RevTree parentTree = parentCommit.getTree();

                // Prepare to insert objects into the repository
                ObjectInserter inserter = repository.newObjectInserter();

                // Create the blob with the file content
                ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, content.getBytes(StandardCharsets.UTF_8));

                // Initialize the tree formatter to create the new tree
                TreeFormatter treeFormatter = new TreeFormatter();
                boolean fileAdded = false;

                // Traverse the parent tree and copy its entries to the new tree
                TreeWalk treeWalk = new TreeWalk(repository);
                treeWalk.addTree(parentTree);
                treeWalk.setRecursive(false);

                /*while (treeWalk.next()) {
                    String entryName = treeWalk.getPathString();
                    FileMode fileMode = treeWalk.getFileMode(0);

                    // If the current entry is the file we want to add, skip adding it from the old tree

                    if (fileMode == FileMode.TREE) {
                        // Recurse into the directory
                        treeFormatter.append(entryName, fileMode, treeWalk.getObjectId(0));
                        treeWalk.enterSubtree();
                    } else {
                        // Copy the existing file or directory to the new tree
                        treeFormatter.append(entryName, fileMode, treeWalk.getObjectId(0));
                    }
                }*/

                // If the file was not present in the previous tree, add it now
                treeFormatter.append(filePath, FileMode.REGULAR_FILE, blobId);

                // Insert the new tree object
                ObjectId treeId = treeFormatter.insertTo(inserter);

                // Create a new commit with the new tree and the parent commit
                CommitBuilder commitBuilder = new CommitBuilder();
                commitBuilder.setTreeId(treeId);
                commitBuilder.setParentIds(parentCommit);
                commitBuilder.setAuthor(new PersonIdent("Your Name", "youremail@example.com"));
                commitBuilder.setCommitter(new PersonIdent("Your Name", "youremail@example.com"));
                commitBuilder.setMessage("Add new file to branch");

                ObjectId commitId = inserter.insert(commitBuilder);
                inserter.flush();

                // Update the branch reference to point to the new commit
                RefUpdate refUpdate = repository.updateRef("refs/heads/" + targetBranch);
                refUpdate.setNewObjectId(commitId);
                refUpdate.update();

                // Push the branch to the remote repository
                Iterable<PushResult> pushResults = git.push()
                        .setRemote("origin")
                        .setRefSpecs(new RefSpec(targetBranch + ":" + targetBranch))
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                        .call();

                for (PushResult result : pushResults) {
                    System.out.println(result.getMessages());
                }
            }


        } catch (Exception e) {
            log.error("error oushit to git", e);
        }


    }


    public void addFile(Project project, Component component, String targetBranch, String changeSummary, String filePath, String content) {
        String sourceBranch = "master";
        targetBranch = "feature/06";

        Path repoDir = getRepoPath(project, component);

        try (Repository repository = Git.open(repoDir.toFile()).getRepository()) {
            // Step 1: Load the commit of the source branch (develop)
            ObjectId sourceBranchObjectId = repository.resolve(sourceBranch);
            RevWalk revWalk = new RevWalk(repository);
            RevCommit sourceCommit = revWalk.parseCommit(sourceBranchObjectId);
            RevTree sourceTree = sourceCommit.getTree();

            // Step 2: Modify the file content - simply creates a blob that is not attached anywhere
            ObjectId newBlobId;
            try (ObjectInserter inserter = repository.newObjectInserter()) {
                newBlobId = inserter.insert(Constants.OBJ_BLOB, content.getBytes(StandardCharsets.UTF_8));
                inserter.flush();
            }

            // Step 3: Traverse the tree and update the file
            ObjectId newTreeId;
            try (ObjectInserter inserter = repository.newObjectInserter()) {
                TreeFormatter treeFormatter = new TreeFormatter();
                TreeWalk treeWalk = new TreeWalk(repository);
                treeWalk.addTree(sourceTree);
                treeWalk.setRecursive(false);

                while (treeWalk.next()) {


                    log.info("File mode " + treeWalk.getFileMode(0));
                    treeFormatter.append(treeWalk.getPathString(),
                            treeWalk.getFileMode(0),
                            treeWalk.getObjectId(0));
                }

                // Rebuild the tree with the modified file
                if (treeWalk.next()) {
                    treeFormatter.append(treeWalk.getNameString(), treeWalk.getFileMode(0), newBlobId);

                } else {
                    treeFormatter.append(treeWalk.getNameString(), REGULAR_FILE, newBlobId);
                    //throw new IOException("File not found in branch: " + filePath);
                }
                newTreeId = inserter.insert(treeFormatter);
                inserter.flush();
            }

            // Step 4: Create a new commit in the target branch
            ObjectId targetBranchObjectId = repository.resolve(targetBranch + "^{commit}");
            CommitBuilder commitBuilder = new CommitBuilder();
            commitBuilder.setTreeId(newTreeId);
            if (targetBranchObjectId != null) {
                commitBuilder.setParentId(targetBranchObjectId);
            } else {
                commitBuilder.setParentId(sourceCommit.getId());  // Use the source commit as the parent if the branch doesn't exist
            }
            commitBuilder.setAuthor(new PersonIdent("Your Name", "your-email@example.com"));
            commitBuilder.setCommitter(new PersonIdent("Your Name", "your-email@example.com"));
            commitBuilder.setMessage(changeSummary);

            ObjectId newCommitId;
            try (ObjectInserter inserter = repository.newObjectInserter()) {
                newCommitId = inserter.insert(commitBuilder);
                inserter.flush();
            }

            // Step 5: Update the target branch to point to the new commit
            RefUpdate refUpdate = repository.updateRef("refs/heads/" + targetBranch);
            refUpdate.setNewObjectId(newCommitId);
            refUpdate.update();

            // Step 6: Push the changes to the remote repository
            try (Git git = new Git(repository)) {
                git.push()
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password))
                        .add(targetBranch)
                        .call();
            } catch (InvalidRemoteException e) {
                throw new RuntimeException(e);
            } catch (TransportException e) {
                throw new RuntimeException(e);
            } catch (GitAPIException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Changes have been pushed to branch: " + targetBranch);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    private Path getRepoPath(Project project, Component component) {
        return Paths.get(storagePath, project.getName(), component.getName());
    }

    private static byte[] createCommitData(RevCommit parentCommit, ObjectId newTreeId, Repository repository,
                                           String authorName, String authorEmail, String commitMessage) throws IOException {
        PersonIdent author = new PersonIdent(authorName, authorEmail);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("tree " + newTreeId.name() + "\n").getBytes(StandardCharsets.UTF_8));
        out.write(("parent " + parentCommit.getId().name() + "\n").getBytes(StandardCharsets.UTF_8));
        out.write(("author " + author.toExternalString() + "\n").getBytes(StandardCharsets.UTF_8));
        out.write(("committer " + author.toExternalString() + "\n").getBytes(StandardCharsets.UTF_8));
        out.write(("\n" + commitMessage + "\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
