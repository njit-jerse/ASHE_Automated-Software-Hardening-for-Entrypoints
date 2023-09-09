package njit.JerSE.models;

public class GPTChatResponse {
    private String id;
    private String object;
    private String created;
    private String model;
    private GPTChoice[] choices;
    private GPTUsage usage;

    public GPTChatResponse() {}

    public GPTChatResponse(String id, String object, String created, String model, GPTChoice[] choices, GPTUsage usage) {
        this.id = id;
        this.object = object;
        this.created = created;
        this.model = model;
        this.choices = choices;
        this.usage = usage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public GPTChoice[] getChoices() {
        return choices;
    }

    public void setChoices(GPTChoice[] choices) {
        this.choices = choices;
    }

    public GPTUsage getUsage() {
        return usage;
    }

    public void setUsage(GPTUsage usage) {
        this.usage = usage;
    }
}
