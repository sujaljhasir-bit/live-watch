package com.cfs.livewatch.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class room {
    private final String code;
    private String hostid;

    private String videoid;
    private boolean playing;

    private double lastupdatetime;
    private long lastupdateat;
    private Map<String, partipant> participants=new LinkedHashMap<>();

    // requests from participants that wait for a Host or Moderator (oldest first)
    private static final long REQUEST_LIFETIME_MS = 2 * 60 * 1000;
    private final Map<String, ChangeRequest> requests = new LinkedHashMap<>();

    public room(String code){
        this.code=code;
        this.lastupdateat=System.currentTimeMillis();
    }
    public synchronized void addParticipant(partipant Participant){
        participants.put(Participant.getId(),Participant);
        if(Participant.getRole()==role.Host){
            hostid=Participant.getId();
        }

    }
    public synchronized partipant getParticipant(String id){
        return participants.get(id);
    }
    public synchronized boolean removeParticipant(String id) {
        return participants.remove(id) != null;
    }
    public synchronized List<partipant> getParticipants() {
        return new ArrayList<>(participants.values());
    }
    public synchronized double getCurrentTime() {
        if (!playing) {
            return lastupdatetime;
        }
        long elapsedMillis = System.currentTimeMillis() - lastupdateat;

        return lastupdatetime + elapsedMillis / 1000.0;
    }
    public synchronized void play(double time) {
        this.lastupdatetime = time;

        this.lastupdateat = System.currentTimeMillis();

        this.playing = true;
    }
    public synchronized void pause(double time) {
        this.lastupdatetime = time;

        this.lastupdateat = System.currentTimeMillis();

        this.playing = false;

    }
    public synchronized void seek(double time) {
        this.lastupdatetime = time;
        this.lastupdateat = System.currentTimeMillis();

    }
    public synchronized void changeVideo(String videoId) {
        this.videoid = videoId;
        this.requests.clear();
        this.playing = false;

        this.lastupdatetime = 0;
        this.lastupdateat = System.currentTimeMillis();
    }
    // the host gives the crown to someone else; the old host stays on as Moderator
    public synchronized boolean transferHost(String fromId, String toId) {
        partipant from = participants.get(fromId);
        partipant to = participants.get(toId);
        if (from == null || to == null || fromId.equals(toId)) {
            return false;
        }
        from.setRole(role.Moderator);
        to.setRole(role.Host);
        hostid = toId;
        return true;
    }

    // called when the host leaves: the longest-present Moderator, else the longest-present person
    public synchronized partipant promoteNextHost() {
        partipant next = null;
        for (partipant p : participants.values()) {
            if (p.getRole() == role.Moderator) {
                next = p;
                break;
            }
        }
        if (next == null && !participants.isEmpty()) {
            next = participants.values().iterator().next();
        }
        if (next == null) {
            hostid = null;
            return null;
        }
        next.setRole(role.Host);
        hostid = next.getId();
        return next;
    }
    // ---------------------------------------------------------- change requests

    // pending requests; anything older than two minutes is dropped first
    public synchronized List<ChangeRequest> getRequests() {
        long now = System.currentTimeMillis();
        requests.values().removeIf(r -> now - r.getCreatedAt() > REQUEST_LIFETIME_MS);
        return new ArrayList<>(requests.values());
    }

    public synchronized void addRequest(ChangeRequest request) {
        requests.put(request.getId(), request);
    }

    public synchronized ChangeRequest getRequest(String id) {
        getRequests(); // drops expired ones
        return requests.get(id);
    }

    public synchronized void removeRequest(String id) {
        requests.remove(id);
    }

    public synchronized void removeRequestsBy(String participantId) {
        requests.values().removeIf(r -> r.getRequesterId().equals(participantId));
    }

    public String getCode() { return code; }
    public synchronized String getHostId() { return hostid; }
    public synchronized String getVideoId() { return videoid; }
    public synchronized boolean isPlaying() { return playing; }
}

