package dev.omyshko.contentmanagement.instructions.model;

public enum INSTRUCTIONS_TYPE {
    CONTENT_UPDATE("content-update"), SCRIPT_EXECUTION("script-execution");

    INSTRUCTIONS_TYPE(String knowledgeFolder) {
        this.knowledgeFolder = knowledgeFolder;
    }

    //This is where all the knowledge and examples for the instruction lies
    private final String knowledgeFolder;

    public String getKnowledgeFolder() {
        return knowledgeFolder;
    }
}
