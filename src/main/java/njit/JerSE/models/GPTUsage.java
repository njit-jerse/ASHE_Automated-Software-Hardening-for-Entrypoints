package njit.JerSE.models;

public record GPTUsage(
        // snake_case is very important for the API
        int prompt_tokens,
        int completion_tokens,
        int total_tokens
) {}