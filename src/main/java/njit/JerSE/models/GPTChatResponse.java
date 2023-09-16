package njit.JerSE.models;

/**
 * Represents a response from the ChatGPT API after initiating a chat conversation.
 * This record encapsulates various details returned by the API after processing a request.
 * <p>
 * When a message or a series of messages are sent to ChatGPT for processing,
 * the API responds with a structured output which this record captures.
 *
 * @param id       the unique identifier for the chat session or message
 * @param object   the type of the object, typically indicating a "chat" or similar category
 * @param created  the timestamp indicating when the response was created
 * @param model    the model version or identifier used by ChatGPT for this response
 * @param choices  an array of {@link GPTChoice} representing potential responses or outputs from ChatGPT
 * @param usage    an instance of {@link GPTUsage} detailing the token usage or computational resources used for the request
 */
public record GPTChatResponse(
        String id,
        String object,
        String created,
        String model,
        GPTChoice[] choices,
        GPTUsage usage
) {}
