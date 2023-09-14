package njit.JerSE.models;

/**
 * Represents the various models provided by the ChatGPT API.
 * <p>
 * This enum encapsulates the parameters associated with each model,
 * allowing for easy selection and configuration when making requests to the ChatGPT API.
 *
 * <p><strong>Note:</strong> It's crucial to maintain field names in snake_case for compatibility with the API.
 */
public enum GPTModel {

    // Further models can be added as they are released or used

    /*
    GPT_3_5_TURBO(
            "gpt-3.5-turbo",
            1.0,
            1000,
            1.0,
            0,
            0
    ),
    */

    GPT_4(
            "gpt-4",
            0.2,
            1000,
            0.1,
            0,
            0
    );

    private final String model;
    private final double temperature;
    private final int max_tokens;
    private final double top_p;
    private final int frequency_penalty;
    private final int presence_penalty;

    /**
     * Constructs a new GPTModel with the given parameters.
     *
     * @param model             The identifier for the GPT model.
     * @param temperature       Controls the randomness of the output.
     * @param max_tokens        Sets the maximum length of the response in terms of tokens.
     * @param top_p             Filters the potential response pool to this fraction of most likely choices.
     * @param frequency_penalty Adjusts the likelihood of tokens based on their frequency in training data.
     * @param presence_penalty  Adjusts the likelihood of tokens based on their presence in the input.
     */
    GPTModel(String model, double temperature, int max_tokens, double top_p, int frequency_penalty, int presence_penalty) {
        this.model = model;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.top_p = top_p;
        this.frequency_penalty = frequency_penalty;
        this.presence_penalty = presence_penalty;
    }

    // Accessor methods with their respective descriptions

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