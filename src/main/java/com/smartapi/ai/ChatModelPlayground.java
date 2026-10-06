package com.smartapi.ai;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exemplo de uso direto do ChatModel (camada mais baixa), sem memória nem tools.
 * Útil para entender Prompt, mensagens e options antes de subir para o ChatClient.
 */
@Service
public class ChatModelPlayground {

    private final ChatModel chatModel;

    public ChatModelPlayground(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String ask(String question, Double temperature) {
        var options = OpenAiChatOptions.builder()
                .temperature(temperature != null ? temperature : 0.7)
                .build();

        Prompt prompt = new Prompt(List.of(
                new SystemMessage("Você é um educador financeiro. Responda em português, em até 3 frases."),
                new UserMessage(question)), options);

        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
