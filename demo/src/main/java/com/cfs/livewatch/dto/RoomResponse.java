package com.cfs.livewatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private String code;
    private String hostId;
    private String videoId;
    private boolean playing;

    /*
     * Video position at serverTime.
     */
    private double currentTime;

    /*
     * Exact server timestamp, milliseconds since epoch.
     *
     * Clients use this to calculate where the video
     * should be RIGHT NOW.
     */
    private long serverTime;

    private List<Participantdto> participants;
}