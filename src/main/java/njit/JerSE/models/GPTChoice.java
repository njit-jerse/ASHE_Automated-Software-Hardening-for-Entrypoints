package njit.JerSE.models;

public class GPTChoice {
    int index;
    GPTMessage message;
    // snake_case is very important for the API
    @com.fasterxml.jackson.annotation.JsonProperty("finish_reason")
    String finish_reason;

    public GPTChoice() {}

    public GPTChoice(int index, GPTMessage message, String finish_reason) {
        this.index = index;
        this.message = message;
        this.finish_reason = finish_reason;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public GPTMessage getMessage() {
        return message;
    }

    public void setMessage(GPTMessage message) {
        this.message = message;
    }

    public String getFinishReason() {
        return finish_reason;
    }

    public void setFinishReason(String finish_reason) {
        this.finish_reason = finish_reason;
    }
}
