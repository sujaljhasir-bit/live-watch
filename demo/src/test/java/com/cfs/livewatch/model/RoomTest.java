package com.cfs.livewatch.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    void hostIsRecordedAndParticipantCannotControl() {
        room myRoom = new room("ABC123");
        partipant alice = new partipant("Alice", role.Host);
        partipant bob = new partipant("Bob", role.Participant);
        myRoom.addparticipant(alice);
        myRoom.addparticipant(bob);

        assertEquals(alice.getId(), myRoom.getHostId());
        assertFalse(bob.getRole().playbackcontrol());

        bob.setRole(role.Moderator);
        assertTrue(bob.getRole().playbackcontrol());
        assertFalse(bob.getRole().managecontrol());
    }

    @Test
    void timeAdvancesWhilePlayingAndFreezesWhenPaused() throws InterruptedException {
        room myRoom = new room("ABC123");
        myRoom.changeVideo("dQw4w9WgXcQ");

        myRoom.play(10);
        Thread.sleep(600);
        double time = myRoom.getCurrentTime();
        assertTrue(time >= 10.5 && time < 11.5, "time was " + time);

        myRoom.pause(20);
        Thread.sleep(300);
        assertEquals(20, myRoom.getCurrentTime());
    }
}