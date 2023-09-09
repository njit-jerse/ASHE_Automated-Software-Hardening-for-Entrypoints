package njit.JerSE.models;

public class GPTRequest {
    // snake_case is very important for the API
    private String model;
    private double temperature;
    private int max_tokens;
    private double top_p;
    private int frequency_penalty;
    private int presence_penalty;
    private GPTMessage[] messages;

    public GPTRequest(String model, double temperature, int max_tokens, double top_p, int frequency_penalty, int presence_penalty, GPTMessage[] messages) {
        this.model = model;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.top_p = top_p;
        this.frequency_penalty = frequency_penalty;
        this.presence_penalty = presence_penalty;
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }

    public double getTop_p() {
        return top_p;
    }

    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }

    public int getFrequency_penalty() {
        return frequency_penalty;
    }

    public void setFrequency_penalty(int frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }

    public int getPresence_penalty() {
        return presence_penalty;
    }

    public void setPresence_penalty(int presence_penalty) {
        this.presence_penalty = presence_penalty;
    }

    public GPTMessage[] getMessages() {
        return messages;
    }

    public void setMessages(GPTMessage[] messages) {
        this.messages = messages;
    }
}
