// The rule for keeping a player in step with the server. It only DECIDES what to do;
// VideoPlayer.jsx does it. Keeping the decision separate makes it easy to test.
//
//   desired = what the server says:   { videoId, playing, time }
//   actual  = what our player is doing: { loadedVideoId, state, time, duration }
export const YT_STATE = { UNSTARTED: -1, ENDED: 0, PLAYING: 1, PAUSED: 2, BUFFERING: 3, CUED: 5 };

const MAX_DRIFT_WHILE_PLAYING = 1.0; // seconds we tolerate before jumping
const MAX_DRIFT_WHILE_PAUSED = 0.5;

export function planSync(desired, actual) {
  if (!desired.videoId) return [];

  // wrong video (or none yet): load it, starting at the right time
  if (actual.loadedVideoId !== desired.videoId) {
    return [{ do: desired.playing ? "load" : "cue", videoId: desired.videoId, time: desired.time }];
  }

  // a video that has finished stays finished, unless the room jumped back into it
  if (actual.state === YT_STATE.ENDED) {
    const jumpedBackInside = actual.duration > 0 && desired.time < actual.duration - 1;
    if (!jumpedBackInside) return [];
  }

  const drift = Math.abs(actual.time - desired.time);
  const commands = [];

  if (desired.playing) {
    if (drift > MAX_DRIFT_WHILE_PLAYING) commands.push({ do: "seek", time: desired.time });
    if (actual.state !== YT_STATE.PLAYING && actual.state !== YT_STATE.BUFFERING) {
      commands.push({ do: "play" });
    }
  } else {
    if (actual.state === YT_STATE.PLAYING || actual.state === YT_STATE.BUFFERING) {
      commands.push({ do: "pause" });
    }
    if (drift > MAX_DRIFT_WHILE_PAUSED) commands.push({ do: "seek", time: desired.time });
  }
  return commands;
}
