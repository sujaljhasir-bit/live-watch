// Where the backend lives.
//  - npm run dev (localhost:5173): the Spring app runs separately, on port 8080.
//  - production build: Spring Boot serves this app itself, so the API and the WebSocket
//    are on the SAME address as the page. That also means no CORS problems.
const dev = import.meta.env.DEV;
const wsScheme = window.location.protocol === "https:" ? "wss" : "ws";

export const API_URL = dev ? "http://localhost:8080" : "";
export const WS_URL = dev ? "ws://localhost:8080/ws" : `${wsScheme}://${window.location.host}/ws`;
