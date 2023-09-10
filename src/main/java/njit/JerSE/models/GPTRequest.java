package njit.JerSE.models;

public record GPTRequest(
        String model,
        double temperature,
        int max_tokens,
        double top_p,
        int frequency_penalty,
        int presence_penalty,
        GPTMessage[] messages
) {}