package io.github.afgprojects.framework.ai.core.memory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConversationMemoryTest {

    @Test
    void message_shouldCreateWithAllFields() {
        Message message = new Message(
            Message.Role.USER,
            "Hello!",
            List.of(),
            List.of(),
            List.of()
        );

        assertEquals(Message.Role.USER, message.role());
        assertEquals("Hello!", message.content());
        assertTrue(message.toolCalls().isEmpty());
        assertTrue(message.toolResults().isEmpty());
        assertTrue(message.media().isEmpty());
    }

    @Test
    void messageRole_shouldHaveAllValues() {
        Message.Role[] roles = Message.Role.values();
        assertEquals(4, roles.length);
    }
}
