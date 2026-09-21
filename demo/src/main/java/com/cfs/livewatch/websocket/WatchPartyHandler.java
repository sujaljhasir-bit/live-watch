package com.cfs.livewatch.websocket;

import com.cfs.livewatch.dto.ChangeRequestDto;
import com.cfs.livewatch.dto.ClientMessage;
import com.cfs.livewatch.dto.RoomResponse;
import com.cfs.livewatch.exception.BadRequestException;
import com.cfs.livewatch.exception.ForbiddenException;
import com.cfs.livewatch.exception.noroomfound;
import com.cfs.livewatch.model.partipant;
import com.cfs.livewatch.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
// KEEP THE ObjectMapper IMPORT YOUR CURRENT FILE ALREADY HAS (Spring Boot 4 uses tools.jackson...,
// Spring Boot 3 uses com.fasterxml.jackson.databind...). If it is red, press Alt+Enter and import it.
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WatchPartyHandler extends TextWebSocketHandler {

    private static final int MAX_CHAT_LENGTH = 300;

    private final RoomService roomService;
    private final ObjectMapper objectMapper;

    // room code -> (participant id -> that person's open connection)
    private final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // ------------------------------------------------------------ connect

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> params = queryParams(session.getUri());
        String code = params.get("room");
        partipant me;
        try {
            // the token proves who this is; a public participant id is NOT accepted here
            me = roomService.authenticate(code, params.get("token"));
        } catch (RuntimeException e) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid room or token"));
            return;
        }
        String roomCode = code.trim().toUpperCase();
        session.getAttributes().put("roomCode", roomCode);
        session.getAttributes().put("participantId", me.getId());
        session.getAttributes().put("username", me.getUser());

        Map<String, WebSocketSession> members = rooms.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>());
        WebSocketSession old = members.put(me.getId(), session);
        if (old != null && old.isOpen()) {
            old.close(); // same person opened a second tab: keep only the newest connection
        }

        broadcast(roomCode, "user_joined", me.getUser(), roomService.getRoom(roomCode));
        broadcastRequests(roomCode); // so a newcomer sees what is already waiting
    }

    // ------------------------------------------------------------ messages

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String roomCode = (String) session.getAttributes().get("roomCode");
        String myId = (String) session.getAttributes().get("participantId");
        String myName = (String) session.getAttributes().get("username");
        if (roomCode == null || myId == null) {
            return;
        }

        ClientMessage msg;
        try {
            msg = objectMapper.readValue(message.getPayload(), ClientMessage.class);
        } catch (Exception e) {
            sendError(session, "unknown", "Message is not valid JSON");
            return;
        }
        if (msg == null || msg.getType() == null) {
            sendError(session, "unknown", "Message needs a type");
            return;
        }

        try {
            switch (msg.getType()) {
                case "play" -> broadcast(roomCode, "sync_state", myName,
                        roomService.play(roomCode, myId, msg.getTime()));
                case "pause" -> broadcast(roomCode, "sync_state", myName,
                        roomService.pause(roomCode, myId, msg.getTime()));
                case "seek" -> broadcast(roomCode, "sync_state", myName,
                        roomService.seek(roomCode, myId, msg.getTime()));
                case "change_video" -> {
                    broadcast(roomCode, "sync_state", myName,
                            roomService.changeVideo(roomCode, myId, msg.getVideoUrl()));
                    broadcastRequests(roomCode); // a new video makes waiting requests out of date
                }
                case "assign_role" -> broadcast(roomCode, "role_assigned", myName,
                        roomService.assignRole(roomCode, myId, msg.getTargetId(), msg.getRole()));
                case "transfer_host" -> broadcast(roomCode, "host_transferred", myName,
                        roomService.transferHost(roomCode, myId, msg.getTargetId()));
                case "remove_participant" -> removeParticipant(roomCode, myId, myName, msg.getTargetId());
                case "request_change" -> requestChange(roomCode, myId, msg);
                case "approve_request" -> approveRequest(roomCode, myId, myName, msg.getRequestId());
                case "decline_request" -> declineRequest(roomCode, myId, myName, msg.getRequestId());
                case "chat" -> chat(session, roomCode, myId, myName, msg.getText());
                case "request_sync" -> send(session, "sync_state", null, roomService.getRoom(roomCode));
                default -> sendError(session, msg.getType(), "Unknown message type");
            }
        } catch (ForbiddenException | BadRequestException | noroomfound e) {
            sendError(session, msg.getType(), e.getMessage());   // only the sender sees this
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, msg.getType(), "Something went wrong");
        }
    }

    private void removeParticipant(String roomCode, String myId, String myName, String targetId) {
        RoomResponse updated = roomService.removeParticipant(roomCode, myId, targetId);

        Map<String, WebSocketSession> members = rooms.get(roomCode);
        WebSocketSession kicked = (members == null) ? null : members.remove(targetId);
        if (kicked != null) {
            send(kicked, "removed", myName, updated);
            try {
                kicked.close(CloseStatus.NORMAL.withReason("Removed by host"));
            } catch (Exception ignored) {
            }
        }
        broadcast(roomCode, "participant_removed", myName, updated);
        broadcastRequests(roomCode); // their waiting requests were dropped
    }

    // ------------------------------------------------- requests that need approval

    private void requestChange(String roomCode, String myId, ClientMessage msg) {
        roomService.requestChange(roomCode, myId, msg.getKind(), msg.getVideoUrl(), msg.getTime());
        broadcastRequests(roomCode);
    }

    private void approveRequest(String roomCode, String myId, String myName, String requestId) {
        ChangeRequestDto request = roomService.approveRequest(roomCode, myId, requestId);
        broadcast(roomCode, "sync_state", myName, roomService.getRoom(roomCode));
        broadcastRequests(roomCode);
        notifyRequester(roomCode, request, "approved", myName);
    }

    private void declineRequest(String roomCode, String myId, String myName, String requestId) {
        ChangeRequestDto request = roomService.declineRequest(roomCode, myId, requestId);
        broadcastRequests(roomCode);
        if (!request.getRequesterId().equals(myId)) { // cancelling your own request needs no answer
            notifyRequester(roomCode, request, "declined", myName);
        }
    }

    private void notifyRequester(String roomCode, ChangeRequestDto request, String outcome, String by) {
        Map<String, WebSocketSession> members = rooms.get(roomCode);
        WebSocketSession target = (members == null) ? null : members.get(request.getRequesterId());
        if (target == null) {
            return; // they left in the meantime
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "request_resolved");
        body.put("outcome", outcome);
        body.put("by", by);
        body.put("request", request);
        sendJson(target, body);
    }

    // everybody gets the full list; the browser decides what each person may see
    private void broadcastRequests(String roomCode) {
        Map<String, WebSocketSession> members = rooms.get(roomCode);
        if (members == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "requests_updated");
        body.put("requests", roomService.listRequests(roomCode));
        for (WebSocketSession s : members.values()) {
            sendJson(s, body);
        }
    }

    // ------------------------------------------------------------ chat

    private void chat(WebSocketSession session, String roomCode, String myId, String myName, String text) {
        if (text == null || text.isBlank()) {
            sendError(session, "chat", "Message is empty");
            return;
        }
        String cleaned = text.trim();
        if (cleaned.length() > MAX_CHAT_LENGTH) {
            sendError(session, "chat", "Message is too long (" + MAX_CHAT_LENGTH + " characters max)");
            return;
        }
        Map<String, WebSocketSession> members = rooms.get(roomCode);
        if (members == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "chat");
        body.put("by", myName);
        body.put("userId", myId);
        body.put("text", cleaned);
        body.put("at", System.currentTimeMillis());
        for (WebSocketSession s : members.values()) {
            sendJson(s, body);
        }
    }

    // ------------------------------------------------------------ disconnect

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomCode = (String) session.getAttributes().get("roomCode");
        String myId = (String) session.getAttributes().get("participantId");
        String myName = (String) session.getAttributes().get("username");
        if (roomCode == null || myId == null) {
            return;
        }
        Map<String, WebSocketSession> members = rooms.get(roomCode);
        // ignore closes of connections that were already replaced or kicked
        if (members == null || members.get(myId) != session) {
            return;
        }
        members.remove(myId);
        if (members.isEmpty()) {
            rooms.remove(roomCode);
        }
        RoomResponse updated = roomService.leave(roomCode, myId);
        if (updated != null) {
            broadcast(roomCode, "user_left", myName, updated);
            broadcastRequests(roomCode); // their waiting requests were dropped
        }
    }

    // ------------------------------------------------------------ sending

    private void broadcast(String roomCode, String type, String by, RoomResponse room) {
        Map<String, WebSocketSession> members = rooms.get(roomCode);
        if (members == null) {
            return;
        }
        for (WebSocketSession s : members.values()) {
            send(s, type, by, room);
        }
    }

    private void send(WebSocketSession session, String type, String by, RoomResponse room) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", type);
        body.put("by", by);
        body.put("room", room);
        sendJson(session, body);
    }

    private void sendError(WebSocketSession session, String event, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "error");
        body.put("event", event);
        body.put("message", message);
        sendJson(session, body);
    }

    private void sendJson(WebSocketSession session, Object body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            synchronized (session) {   // two threads must never write to one session at the same time
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> queryParams(URI uri) {
        Map<String, String> result = new HashMap<>();
        if (uri == null || uri.getQuery() == null) {
            return result;
        }
        for (String pair : uri.getQuery().split("&")) {
            String[] keyAndValue = pair.split("=", 2);
            if (keyAndValue.length == 2) {
                result.put(keyAndValue[0], keyAndValue[1]);
            }
        }
        return result;
    }
}
