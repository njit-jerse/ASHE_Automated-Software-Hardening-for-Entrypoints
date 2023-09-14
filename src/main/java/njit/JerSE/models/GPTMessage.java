package njit.JerSE.models;

/**
 * Represents a message to or from the ChatGPT API.
 * <p>
 * A message encapsulates content either sent to the API as input or received from the API as a response.
 *
 * @param role    The role of the message, typically indicating whether it's a 'system', 'user', or 'assistant' message.
 * @param content The textual content of the message.
 */
public record GPTMessage(
        String role,
        String content
) {}