package njit.JerSE.models;

/**
 * TODO: This documentation says "Represents a request", but the `messages` field seems to represent the whole history of the conversation.  Please clarify.
 * Represents a request made to the ChatGPT API.
 * <p>
 * Each request contains a variety of parameters that influence the response from ChatGPT.
 *
 * <p><strong>Note:</strong> It's important that the field names remain in snake_case for API compatibility.
 *
 * @param model             the identifier for the model version used by ChatGPT
 * @param temperature       controls the randomness of the output; a higher value makes the output more random
 * @param max_tokens        the maximum length of the response in terms of tokens
 * @param top_p             filters the potential response pool to this percentage of most likely choices
 * @param frequency_penalty adjusts the likelihood of tokens appearing based on their frequency in the model's training data
 * @param presence_penalty  adjusts the likelihood of tokens appearing based on their presence in the input message
 * @param messages          an array of {@link GPTMessage} representing the conversation history or context
 */
public record GPTRequest(
        // TODO: Could the first 6 fields be a GPTModel?
        String model,
        double temperature,
        int max_tokens,
        double top_p,
        int frequency_penalty,
        int presence_penalty,
        GPTMessage[] messages
) {}
