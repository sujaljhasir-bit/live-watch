package com.cfs.livewatch.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientMessage {
    private String type;      // play, pause, seek, change_video, assign_role, chat, request_change, ...
    private Double time;
    private String videoUrl;
    private String targetId;
    private String role;
    private String text;      // chat
    private String kind;      // request_change: change_video | play | pause | seek
    private String requestId; // approve_request / decline_request
}



