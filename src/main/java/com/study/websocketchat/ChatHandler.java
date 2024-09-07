package com.study.websocketchat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHandler extends TextWebSocketHandler {
    private static final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> roomHistories = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        Message incomingMessage = objectMapper.readValue(payload, Message.class);

        String room = incomingMessage.getRoom();
        String name = incomingMessage.getName();
        String text = incomingMessage.getMessage();
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String formattedMessage = "[" + time + "] " + name + ": " + text;

        roomHistories.computeIfAbsent(room, k -> new ArrayList<>()).add(formattedMessage);
        if (rooms.containsKey(room)) {
            for (WebSocketSession webSocketSession : rooms.get(room)) {
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(new TextMessage(formattedMessage));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        rooms.values().forEach(sessions -> sessions.remove(session));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = Objects.requireNonNull(session.getUri()).toString();
        String room = uri.split("/chat/")[1];
        rooms.computeIfAbsent(room, k -> new HashSet<>()).add(session);
        List<String> history = roomHistories.get(room);
        if (history != null) {
            for (String pastMessage : history) {
                session.sendMessage(new TextMessage(pastMessage));
            }
        }
    }
}
