package io.github.afgprojects.framework.ai.llm.advisor;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.Retriever;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Advisor adapter for RAG (Retrieval-Augmented Generation) operations.
 * <p>
 * This adapter wraps the framework's Retriever interface and provides
 * automatic retrieval of relevant documents and query augmentation.
 *
 * <p>Example usage:
 * <pre>{@code
 * Retriever retriever = new VectorStoreRetriever(vectorStore, embeddingModel);
 * RagAdvisorAdapter advisor = new RagAdvisorAdapter(retriever, 5);
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class RagAdvisorAdapter implements LlmAdvisor {

    private final Retriever retriever;
    private final int topK;
    private final ContextFormatter contextFormatter;
    private final boolean includeMetadata;

    /**
     * Creates an advisor with default settings.
     *
     * @param retriever the document retriever
     */
    public RagAdvisorAdapter(@NonNull Retriever retriever) {
        this(retriever, 5, new DefaultContextFormatter(), false);
    }

    /**
     * Creates an advisor with custom top-K.
     *
     * @param retriever the document retriever
     * @param topK      the number of documents to retrieve
     */
    public RagAdvisorAdapter(@NonNull Retriever retriever, int topK) {
        this(retriever, topK, new DefaultContextFormatter(), false);
    }

    /**
     * Creates an advisor with custom settings.
     *
     * @param retriever       the document retriever
     * @param topK            the number of documents to retrieve
     * @param contextFormatter the formatter for context presentation
     * @param includeMetadata whether to include document metadata in context
     */
    public RagAdvisorAdapter(
            @NonNull Retriever retriever,
            int topK,
            @NonNull ContextFormatter contextFormatter,
            boolean includeMetadata) {
        this.retriever = retriever;
        this.topK = topK;
        this.contextFormatter = contextFormatter;
        this.includeMetadata = includeMetadata;
    }

    @Override
    public int getOrder() {
        // Execute after memory advisor to augment the actual user query
        return 200;
    }

    @Override
    public String getName() {
        return "RagAdvisorAdapter";
    }

    @Override
    @NonNull
    public List<org.springframework.ai.chat.messages.Message> apply(
            @NonNull List<org.springframework.ai.chat.messages.Message> messages,
            @NonNull AdvisorContext context) {
        // Find the last user message to use as query
        String query = extractUserQuery(messages);
        if (query == null || query.isBlank()) {
            return messages;
        }

        // Retrieve relevant documents
        List<Document> documents = retriever.retrieve(query, topK);
        if (documents.isEmpty()) {
            return messages;
        }

        // Format context from documents
        String ragContext = contextFormatter.format(documents, includeMetadata);

        // Augment the user message with context
        List<org.springframework.ai.chat.messages.Message> augmentedMessages = new ArrayList<>();
        for (int i = 0; i < messages.size() - 1; i++) {
            augmentedMessages.add(messages.get(i));
        }

        // Add augmented last user message
        String augmentedQuery = buildAugmentedQuery(query, ragContext);
        augmentedMessages.add(new UserMessage(augmentedQuery));

        return augmentedMessages;
    }

    /**
     * Extracts the user query from the last user message.
     */
    @Nullable
    private String extractUserQuery(List<org.springframework.ai.chat.messages.Message> messages) {
        if (messages.isEmpty()) {
            return null;
        }

        var lastMessage = messages.get(messages.size() - 1);
        if (lastMessage instanceof UserMessage userMessage) {
            return userMessage.getText();
        }

        return null;
    }

    /**
     * Builds the augmented query with context.
     */
    @NonNull
    private String buildAugmentedQuery(@NonNull String query, @NonNull String context) {
        return """
                Based on the following context, please answer the question.

                Context:
                %s

                Question: %s

                Please provide a comprehensive answer based on the context above. If the context doesn't contain enough information to fully answer the question, please indicate what additional information would be helpful.
                """.formatted(context, query);
    }

    /**
     * Functional interface for formatting retrieved context.
     */
    @FunctionalInterface
    public interface ContextFormatter {
        /**
         * Formats the retrieved documents into context string.
         *
         * @param documents       the retrieved documents
         * @param includeMetadata whether to include metadata
         * @return the formatted context string
         */
        @NonNull
        String format(@NonNull List<Document> documents, boolean includeMetadata);
    }

    /**
     * Default context formatter implementation.
     */
    public static class DefaultContextFormatter implements ContextFormatter {

        @Override
        @NonNull
        public String format(@NonNull List<Document> documents, boolean includeMetadata) {
            StringBuilder sb = new StringBuilder();
            sb.append("---\n");

            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                sb.append("Document ").append(i + 1).append(":\n");
                sb.append(doc.content()).append("\n");

                if (includeMetadata && !doc.metadata().isEmpty()) {
                    sb.append("Metadata: ").append(doc.metadata()).append("\n");
                }

                sb.append("---\n");
            }

            return sb.toString();
        }
    }

    /**
     * Creates a context formatter that uses a custom template.
     *
     * @param template the template string with {content} and {metadata} placeholders
     * @return the custom context formatter
     */
    @NonNull
    public static ContextFormatter customTemplate(@NonNull String template) {
        return (documents, includeMetadata) -> {
            StringBuilder sb = new StringBuilder();
            for (Document doc : documents) {
                String entry = template
                        .replace("{content}", doc.content())
                        .replace("{metadata}", includeMetadata ? doc.metadata().toString() : "");
                sb.append(entry).append("\n");
            }
            return sb.toString();
        };
    }
}
