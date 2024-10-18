package dev.omyshko.contentmanagement.instructions.model;

public enum RESPONSE_FORMAT {
    CHANGE_LOG("change-log"), SCRIPT_EXECUTION("script-execution");

    RESPONSE_FORMAT(String knowledgeFolder) {
        this.knowledgeFolder = knowledgeFolder;
    }

    //This is where all the knowledge and examples for the instruction lies
    private final String knowledgeFolder;

    public String getKnowledgeFolder() {
        return knowledgeFolder;
    }
}
