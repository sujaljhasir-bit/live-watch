package com.cfs.livewatch.controller;
import com.cfs.livewatch.dto.Createroomrequest;
import com.cfs.livewatch.dto.JoinResponse;
import com.cfs.livewatch.dto.Joinroomrequest;
import com.cfs.livewatch.dto.RoomResponse;
import com.cfs.livewatch.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class Streamcontroller {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<JoinResponse> createRoom(@Valid @RequestBody Createroomrequest request) {
        return new ResponseEntity<>(roomService.createRoom(request.getUsername()), HttpStatus.CREATED);
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<JoinResponse> joinRoom(@PathVariable String code,
                                                 @Valid @RequestBody Joinroomrequest request) {
        return ResponseEntity.ok(roomService.joinRoom(code, request.getUsername()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        return ResponseEntity.ok(roomService.getRoom(code));
    }
}
