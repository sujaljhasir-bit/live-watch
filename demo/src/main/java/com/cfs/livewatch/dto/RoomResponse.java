package com.cfs.livewatch.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.management.relation.Role;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private String code;
    private String hostId;
    private String videoId;
    private boolean playing;
    private double currentTime;
    private List<Participantdto> participants;
}
