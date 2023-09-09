package njit.JerSE.models;

public enum GPTModel {
    // TODO: Add more models to utilize and fine tune
    GPT_3_5_TURBO(
            "gpt-3.5-turbo",
            1.0,
            1000,
            1.0,
            0,
            0
    );
    // Example model
    // GPT_4("gpt-4", 1.0, 1000, 1.0, 0, 0)

    // snake_case is very important for the API
    private final String model;
    private final double temperature;
    private final int max_tokens;
    private final double top_p;
    private final int frequency_penalty;
    private final int presence_penalty;

    GPTModel(String model, double temperature, int max_tokens, double top_p, int frequency_penalty, int presence_penalty) {
        this.model = model;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.top_p = top_p;
        this.frequency_penalty = frequency_penalty;
        this.presence_penalty = presence_penalty;
    }

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public double getTop_p() {
        return top_p;
    }

    public int getFrequency_penalty() {
        return frequency_penalty;
    }

    public int getPresence_penalty() {
        return presence_penalty;
    }
}