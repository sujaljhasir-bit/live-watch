import { useCallback, useEffect, useRef, useState } from "react";
import { loadYouTubeApi } from "./youtube";
import { planSync } from "./syncLogic";

const ERROR_TEXT = {
  2: "That video link is not valid.",
  5: "The player ran into a problem with this video.",
  100: "This video was not found, or it is private.",
  101: "The owner of this video does not allow it to be embedded.",
  150: "The owner of this video does not allow it to be embedded.",
};

// Shows the YouTube player and makes it OBEY the server. Nobody can click the video:
// a transparent shield sits on top of it and the native controls are switched off, so
// the only way to change playback is a message to the server (see Controls.jsx).
export default function VideoPlayer({ videoId, playing, time, version, onTick }) {
  const containerRef = useRef(null);
  const playerRef = useRef(null);
  const readyRef = useRef(false);
  const loadedIdRef = useRef(null);
  const desiredRef = useRef({ videoId, playing, time });
  const [blocked, setBlocked] = useState(false); // the browser refused to autoplay
  const [problem, setProblem] = useState("");

  const syncNow = useCallback(() => {
    const player = playerRef.current;
    if (!player || !readyRef.current) return;

    const actual = {
      loadedVideoId: loadedIdRef.current,
      state: player.getPlayerState(),
      time: player.getCurrentTime(),
      duration: player.getDuration(),
    };

    for (const cmd of planSync(desiredRef.current, actual)) {
      if (cmd.do === "load") {
        loadedIdRef.current = cmd.videoId;
        setProblem("");
        player.loadVideoById({ videoId: cmd.videoId, startSeconds: cmd.time });
      } else if (cmd.do === "cue") {
        loadedIdRef.current = cmd.videoId;
        setProblem("");
        player.cueVideoById({ videoId: cmd.videoId, startSeconds: cmd.time });
      } else if (cmd.do === "seek") {
        player.seekTo(cmd.time, true);
      } else if (cmd.do === "play") {
        player.playVideo();
      } else if (cmd.do === "pause") {
        player.pauseVideo();
      }
    }
  }, []);

  // create the player once
  useEffect(() => {
    let cancelled = false;
    let player = null;

    loadYouTubeApi().then((YT) => {
      if (cancelled || !containerRef.current) return;

      // YouTube replaces the element we give it, so give it a throwaway one
      const holder = document.createElement("div");
      containerRef.current.appendChild(holder);

      player = new YT.Player(holder, {
        width: "100%",
        height: "100%",
        playerVars: {
          controls: 0,
          disablekb: 1,
          modestbranding: 1,
          rel: 0,
          playsinline: 1,
          iv_load_policy: 3,
          origin: window.location.origin,
        },
        events: {
          onReady: () => {
            readyRef.current = true;
            syncNow();
          },
          onAutoplayBlocked: () => setBlocked(true),
          onError: (event) => setProblem(ERROR_TEXT[event.data] || "This video cannot be played."),
        },
      });
      playerRef.current = player;
    });

    return () => {
      cancelled = true;
      readyRef.current = false;
      loadedIdRef.current = null;
      playerRef.current = null;
      if (player && player.destroy) player.destroy();
    };
  }, [syncNow]);

  // every time the server tells us the room state, line the player up with it
  useEffect(() => {
    desiredRef.current = { videoId, playing, time };
    syncNow();
  }, [videoId, playing, time, version, syncNow]);

  // report the player's position a few times a second, for the slider
  useEffect(() => {
    const id = setInterval(() => {
      const player = playerRef.current;
      if (!player || !readyRef.current || !loadedIdRef.current) return;
      onTick({ time: player.getCurrentTime(), duration: player.getDuration() });
    }, 250);
    return () => clearInterval(id);
  }, [onTick]);

  function startPlayback() {
    setBlocked(false);
    if (playerRef.current) playerRef.current.playVideo();
    setTimeout(syncNow, 600); // then jump to where the room is now
  }

  return (
    <div className="stage">
      <div ref={containerRef} className="stage-player" />
      <div className="stage-shield" />
      {!videoId && (
        <div className="stage-message">
          <span>No Video</span>
        </div>
      )}
      {videoId && problem && <div className="stage-message stage-problem">{problem}</div>}
      {blocked && (
        <button className="stage-start" onClick={startPlayback}>
          Click to join the playback
        </button>
      )}
    </div>
  );
}
