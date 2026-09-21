package com.cfs.livewatch.service;
import com.cfs.livewatch.dto.ChangeRequestDto;
import com.cfs.livewatch.dto.JoinResponse;
import com.cfs.livewatch.model.role;

import com.cfs.livewatch.dto.Participantdto;
import com.cfs.livewatch.dto.RoomResponse;
import com.cfs.livewatch.exception.BadRequestException;
import com.cfs.livewatch.exception.ForbiddenException;
import com.cfs.livewatch.exception.noroomfound;
import com.cfs.livewatch.model.ChangeRequest;
import com.cfs.livewatch.model.partipant;
import com.cfs.livewatch.model.room;
import com.cfs.livewatch.repo.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RoomService {

    private static final String Codechar = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int Codelength = 6;
    private static final int Maxparticipant = 50;
    private static final Pattern Videoid = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    private room findroom(String code) {
        if (code == null) {
            throw new noroomfound("Room code is required");
        }
        String cleaned = code.trim().toUpperCase();
        return roomRepository.findCode(cleaned)
                .orElseThrow(() -> new noroomfound("Room " + cleaned + " not found"));
    }

    private String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Codelength; i++) {
                sb.append(Codechar.charAt(random.nextInt(Codechar.length())));
            }
            code = sb.toString();
        } while (roomRepository.ExistsCode(code));
        return code;
    }

    private final RoomRepository roomRepository;
    private final SecureRandom random = new SecureRandom();

    private Participantdto toParticipantDto(partipant participant) {
        return new Participantdto(participant.getId(), participant.getUser(), participant.getRole());
    }

    private RoomResponse toResponse(room myRoom) {
        List<Participantdto> participants = myRoom.getParticipants().stream()
                .map(this::toParticipantDto)
                .toList();
        return new RoomResponse(
                myRoom.getCode(),
                myRoom.getHostId(),
                myRoom.getVideoId(),
                myRoom.isPlaying(),
                myRoom.getCurrentTime(),
                participants);
    }

    // Look the person up FIRST, then check the role. The other order would call
    // getRole() on null for someone who is not in the room.
    private partipant getParticipantOrThrow(room myRoom, String participantId) {
        partipant found = (participantId == null) ? null : myRoom.getParticipant(participantId);
        if (found == null) {
            throw new ForbiddenException("You are not in this room");
        }
        return found;
    }

    private void requireCanControl(room myRoom, String participantId) {
        partipant who = getParticipantOrThrow(myRoom, participantId);
        if (!who.getRole().playbackcontrol()) {
            throw new ForbiddenException("Your role (" + who.getRole() + ") cannot control playback");
        }
    }

    private void requireVideo(room myRoom) {
        if (myRoom.getVideoId() == null) {
            throw new BadRequestException("Choose a video first");
        }
    }

    public RoomResponse play(String code, String participantId, Double time) {
        room myRoom = findroom(code);
        requireCanControl(myRoom, participantId);
        requireVideo(myRoom);
        myRoom.play(time != null ? time : myRoom.getCurrentTime());
        return toResponse(myRoom);
    }

    public RoomResponse pause(String code, String participantId, Double time) {
        room myRoom = findroom(code);
        requireCanControl(myRoom, participantId);
        requireVideo(myRoom);
        myRoom.pause(time != null ? time : myRoom.getCurrentTime());
        return toResponse(myRoom);
    }

    public RoomResponse seek(String code, String participantId, Double time) {
        room myRoom = findroom(code);
        requireCanControl(myRoom, participantId);
        requireVideo(myRoom);
        if (time == null) {
            throw new BadRequestException("Seek needs a time in seconds");
        }
        myRoom.seek(time);
        return toResponse(myRoom);
    }
    public JoinResponse createRoom(String username) {
        room myRoom = new room(generateCode());
        partipant host = new partipant(username.trim(), role.Host);
        myRoom.addParticipant(host);
        roomRepository.save(myRoom);
        return new JoinResponse(host.getId(), host.getToken(), host.getRole(), toResponse(myRoom));
    }

    public JoinResponse joinRoom(String code, String username) {
        room myRoom = findroom(code);
        if (myRoom.getParticipants().size() >= Maxparticipant) {
            throw new BadRequestException("This room is full");
        }
        partipant guest = new partipant(username.trim(), role.Participant);
        myRoom.addParticipant(guest);
        return new JoinResponse(guest.getId(), guest.getToken(), guest.getRole(), toResponse(myRoom));
    }

    public RoomResponse getRoom(String code) {
        return toResponse(findroom(code));
    }

    public RoomResponse changeVideo(String code, String participantId, String videoUrl) {
        room myRoom = findroom(code);
        partipant who = getParticipantOrThrow(myRoom, participantId);
        if (!who.getRole().changecontrol()) {
            throw new ForbiddenException("Your role (" + who.getRole() + ") cannot change the video");
        }
        myRoom.changeVideo(extractVideoId(videoUrl));
        return toResponse(myRoom);
    }

    private String extractVideoId(String input) {
        if (input == null || input.isBlank()) {
            throw new BadRequestException("Video link is required");
        }
        String text = input.trim();
        if (Videoid.matcher(text).matches()) {
            return text; // user pasted only the ID
        }
        if (!text.startsWith("http")) {
            text = "https://" + text; // "youtube.com/watch?v=..." has no host until we add this
        }

        URI uri;
        try {
            uri = new URI(text);
        } catch (URISyntaxException e) {
            throw new BadRequestException("That is not a valid link");
        }
        if (uri.getHost() == null) {
            throw new BadRequestException("That is not a valid YouTube link");
        }

        String host = uri.getHost().toLowerCase().replaceFirst("^(www|m)\\.", "");
        String path = (uri.getPath() == null) ? "" : uri.getPath();
        String candidate = null;

        if (host.equals("youtu.be")) {
            String[] parts = path.split("/");            // "/abc" -> ["", "abc"]
            if (parts.length > 1) {
                candidate = parts[1];
            }
        } else if (host.equals("youtube.com")) {
            if (path.equals("/watch")) {
                candidate = queryParam(uri.getQuery(), "v");
            } else if (path.startsWith("/embed/") || path.startsWith("/shorts/") || path.startsWith("/live/")) {
                String[] parts = path.split("/");        // "/embed/abc" -> ["", "embed", "abc"]
                if (parts.length > 2) {                  // "/embed/" has only 2 parts, so check first
                    candidate = parts[2];
                }
            }
        }

        if (candidate == null || !Videoid.matcher(candidate).matches()) {
            throw new BadRequestException("Could not find a YouTube video in that link");
        }
        return candidate;
    }

    private String queryParam(String query, String name) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] keyAndValue = pair.split("=", 2);
            if (keyAndValue.length == 2 && keyAndValue[0].equals(name)) {
                return keyAndValue[1];
            }
        }
        return null;
    }
    // Who is holding this token? Throws if the room or token is wrong.
    public partipant authenticate(String code, String token) {
        room myRoom = findroom(code);
        if (token != null) {
            for (partipant p : myRoom.getParticipants()) {
                if (p.getToken().equals(token)) {
                    return p;
                }
            }
        }
        throw new ForbiddenException("Invalid room or token");
    }

    public RoomResponse assignRole(String code, String actorId, String targetId, String roleName) {
        room myRoom = findroom(code);
        partipant actor = getParticipantOrThrow(myRoom, actorId);
        if (!actor.getRole().managecontrol()) {
            throw new ForbiddenException("Only the host can assign roles");
        }
        role newRole = null;
        for (role r : role.values()) {
            if (r.name().equalsIgnoreCase(roleName)) {
                newRole = r;
            }
        }
        if (newRole == null || newRole == role.Host) {
            throw new BadRequestException("Role must be Moderator or Participant");
        }
        partipant target = myRoom.getParticipant(targetId);
        if (target == null) {
            throw new BadRequestException("That person is not in the room");
        }
        if (target.getId().equals(actor.getId())) {
            throw new BadRequestException("You cannot change your own role");
        }
        target.setRole(newRole);
        return toResponse(myRoom);
    }

    public RoomResponse removeParticipant(String code, String actorId, String targetId) {
        room myRoom = findroom(code);
        partipant actor = getParticipantOrThrow(myRoom, actorId);
        if (!actor.getRole().managecontrol()) {
            throw new ForbiddenException("Only the host can remove people");
        }
        if (myRoom.getParticipant(targetId) == null) {
            throw new BadRequestException("That person is not in the room");
        }
        if (targetId.equals(actor.getId())) {
            throw new BadRequestException("You cannot remove yourself");
        }
        myRoom.removeParticipant(targetId);
        myRoom.removeRequestsBy(targetId);
        return toResponse(myRoom);
    }

    public RoomResponse transferHost(String code, String actorId, String targetId) {
        room myRoom = findroom(code);
        partipant actor = getParticipantOrThrow(myRoom, actorId);
        if (!actor.getRole().managecontrol()) {
            throw new ForbiddenException("Only the host can transfer the host role");
        }
        if (!myRoom.transferHost(actor.getId(), targetId)) {
            throw new BadRequestException("Choose another person in the room");
        }
        return toResponse(myRoom);
    }

    // Returns the updated room to broadcast, or null when there is nothing to broadcast
    // (person already gone, or the room is now empty and was deleted).
    public RoomResponse leave(String code, String participantId) {
        room myRoom = roomRepository.findCode(code).orElse(null);
        if (myRoom == null) {
            return null;
        }
        partipant leaver = myRoom.getParticipant(participantId);
        if (leaver == null) {
            return null;
        }
        boolean wasHost = leaver.getRole() == role.Host;
        myRoom.removeParticipant(participantId);
        myRoom.removeRequestsBy(participantId);
        if (myRoom.getParticipants().isEmpty()) {
            roomRepository.deleteCode(code);
            return null;
        }
        if (wasHost) {
            myRoom.promoteNextHost();
        }
        return toResponse(myRoom);
    }

    // ------------------------------------------- requests that need approval

    private static final int MAX_PENDING_PER_PERSON = 3;

    // Someone WITHOUT control rights asks for a change. Nothing happens to the video yet.
    public ChangeRequestDto requestChange(String code, String requesterId, String kind, String videoUrl, Double time) {
        room myRoom = findroom(code);
        partipant who = getParticipantOrThrow(myRoom, requesterId);
        if (who.getRole().playbackcontrol()) {
            throw new BadRequestException("You can do this directly, no approval is needed");
        }
        if (kind == null) {
            throw new BadRequestException("Request needs a kind");
        }

        String videoId = null;
        Double at = null;
        switch (kind) {
            case ChangeRequest.CHANGE_VIDEO -> videoId = extractVideoId(videoUrl);
            case ChangeRequest.PLAY, ChangeRequest.PAUSE -> requireVideo(myRoom);
            case ChangeRequest.SEEK -> {
                requireVideo(myRoom);
                if (time == null || !Double.isFinite(time) || time < 0) {
                    throw new BadRequestException("Seek needs a time in seconds");
                }
                at = time;
            }
            default -> throw new BadRequestException("Unknown request type");
        }

        long mine = myRoom.getRequests().stream().filter(r -> r.getRequesterId().equals(requesterId)).count();
        if (mine >= MAX_PENDING_PER_PERSON) {
            throw new BadRequestException("You already have " + MAX_PENDING_PER_PERSON + " requests waiting");
        }

        ChangeRequest request = new ChangeRequest(who.getId(), who.getUser(), kind, videoId, at);
        myRoom.addRequest(request);
        return toRequestDto(request);
    }

    // A Host or Moderator says yes: the change is applied to the room for everyone.
    public ChangeRequestDto approveRequest(String code, String approverId, String requestId) {
        room myRoom = findroom(code);
        requireCanControl(myRoom, approverId);
        ChangeRequest request = myRoom.getRequest(requestId);
        if (request == null) {
            throw new BadRequestException("That request no longer exists");
        }
        switch (request.getKind()) {
            case ChangeRequest.CHANGE_VIDEO -> myRoom.changeVideo(request.getVideoId());
            case ChangeRequest.PLAY -> myRoom.play(myRoom.getCurrentTime());
            case ChangeRequest.PAUSE -> myRoom.pause(myRoom.getCurrentTime());
            case ChangeRequest.SEEK -> myRoom.seek(request.getTime());
            default -> throw new BadRequestException("Unknown request type");
        }
        myRoom.removeRequest(requestId);
        return toRequestDto(request);
    }

    // A Host/Moderator says no, or the requester takes their own request back.
    public ChangeRequestDto declineRequest(String code, String actorId, String requestId) {
        room myRoom = findroom(code);
        partipant actor = getParticipantOrThrow(myRoom, actorId);
        ChangeRequest request = myRoom.getRequest(requestId);
        if (request == null) {
            throw new BadRequestException("That request no longer exists");
        }
        boolean isOwnRequest = request.getRequesterId().equals(actor.getId());
        if (!isOwnRequest && !actor.getRole().playbackcontrol()) {
            throw new ForbiddenException("Only the host or a moderator can decline requests");
        }
        myRoom.removeRequest(requestId);
        return toRequestDto(request);
    }

    public List<ChangeRequestDto> listRequests(String code) {
        return findroom(code).getRequests().stream().map(this::toRequestDto).toList();
    }

    private ChangeRequestDto toRequestDto(ChangeRequest r) {
        return new ChangeRequestDto(r.getId(), r.getRequesterId(), r.getRequesterName(),
                r.getKind(), r.getVideoId(), r.getTime(), r.getCreatedAt());
    }
}


