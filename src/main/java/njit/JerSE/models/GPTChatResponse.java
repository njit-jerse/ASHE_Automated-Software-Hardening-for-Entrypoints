package njit.JerSE.models;

/**
 * Represents a response from the ChatGPT API after initiating a chat conversation.
 * This record encapsulates various details returned by the API after processing a request.
 * <p>
 * When a message or a series of messages are sent to ChatGPT for processing,
 * the API responds with a structured output which this record captures.
 *
 * TODO: Throughout, when documenting a record, describe the fields with Javadoc on the fields themselves, not on the record class.
 * @param id       the unique identifier for the chat session or message
 * @param object   the type of the object, typically indicating a "chat" or similar category
 * @param created  the timestamp indicating when the response was created
 * @param model    the model version or identifier used by ChatGPT for this response
 * TODO: Documentation does not need to restate the type, which is obvious from the code and/or Javadoc.  For example, you can delete "an array of {@link GPTChoice} representing" and change "an instance of {@link GPTUsage} detailing" to "details"
 * @param choices  an array of {@link GPTChoice} representing potential responses or outputs from ChatGPT
 * @param usage    an instance of {@link GPTUsage} detailing the token usage or computational resources used for the request
 */
// TODO: Why is this named "GPTChatResponse" rather than "ChatGPTResponse"?
public record GPTChatResponse(
        // TODO: it's inconsistent that this file uses 8-space indentation whereas other files use 4 spaces.  We should probably use a formatter like Spotless, but only after addressing all open code reviews, to avoid conflicts.
        String id,
        String object,
        String created,
        String model,
        GPTChoice[] choices,
        GPTUsage usage
) {}
