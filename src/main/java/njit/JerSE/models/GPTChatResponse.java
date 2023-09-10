package njit.JerSE.models;

public record GPTChatResponse(
        String id,
        String object,
        String created,
        String model,
        GPTChoice[] choices,
        GPTUsage usage
) {}