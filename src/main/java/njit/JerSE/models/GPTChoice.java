package njit.JerSE.models;

public record GPTChoice(
        int index,
        GPTMessage message,

        String finish_reason
) {}