package com.cfs.livewatch.model;

import lombok.Getter;

import java.util.UUID;

// A participant's suggestion that a Host or Moderator has to approve before it happens.
@Getter
public class ChangeRequest {

    public static final String CHANGE_VIDEO = "change_video";
    public static final String PLAY = "play";
    public static final String PAUSE = "pause";
    public static final String SEEK = "seek";

    private final String id = UUID.randomUUID().toString();
    private final String requesterId;
    private final String requesterName;
    private final String kind;      // one of the four constants above
    private final String videoId;   // only used by change_video
    private final Double time;      // only used by seek
    private final long createdAt = System.currentTimeMillis();

    public ChangeRequest(String requesterId, String requesterName, String kind, String videoId, Double time) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.kind = kind;
        this.videoId = videoId;
        this.time = time;
    }
}
