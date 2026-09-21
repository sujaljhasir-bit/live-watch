const dev = import.meta.env.DEV;

const API_URL =
  import.meta.env.VITE_API_URL ||
  (dev ? "http://localhost:8080" : "");

const WS_URL =
  import.meta.env.VITE_WS_URL ||
  (dev
    ? "ws://localhost:8080/ws"
    : `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.host}/ws`);

export { API_URL, WS_URL };