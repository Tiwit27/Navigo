import "./envConfig.js";
import express from 'express';
import cors from "cors";
import http from "http";
import WebSocket from "ws";

import authsRoutes from "./routes/auths.js";
import beaconsRoutes from "./routes/beacons.js";
import connectionsRoutes from "./routes/connections.js";
import floorsRoutes from "./routes/floors.js";
import institutionsRoutes from "./routes/institutions.js";
import roomsRoutes from "./routes/rooms.js";


const app = express();
const port = 8800;

const server = http.createServer(app);

const wss = new WebSocket.Server({server, path: "/ws"});

let clients = []; 
let lastOffer = null; 
let lastIceFromAndroid = [];

app.use(cors({
    origin: "*",
    methods: ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"]
}));
app.use(express.urlencoded({extended: true}));
app.use(express.json());

app.use("/api/auths", authsRoutes);
app.use("/api/beacons", beaconsRoutes);
app.use("/api/connections", connectionsRoutes);
app.use("/api/floors", floorsRoutes);
app.use("/api/institutions", institutionsRoutes);
app.use("/api/rooms", roomsRoutes);


wss.on('connection', (ws) => {
  clients.push(ws);
  console.log('Client connected');

  if (lastOffer) {
    console.log('Sending last offer to new client');
    ws.send(JSON.stringify(lastOffer));
    lastIceFromAndroid.forEach(cand => ws.send(JSON.stringify(cand)));
  }

    ws.on('message', (message) => {
        const data = JSON.parse(message.toString());
        console.log('Received message from client:', data);

        if (data.type === "viewer-ready") {
    console.log("Viewer is ready, forwarding to Android");

    clients.forEach(client => {
        if (client !== ws && client.readyState === WebSocket.OPEN) {
        client.send(JSON.stringify({ type: "viewer-ready" }));
        }
    });

    return;
    }


    if (data.type === 'offer') {
      lastOffer = data;
      lastIceFromAndroid = [];
    }

    if (data.type === 'ice' && data.from === 'android') {
      lastIceFromAndroid.push(data);
    }

    clients.forEach(client => {
      if (client !== ws && client.readyState === WebSocket.OPEN) {
        client.send(JSON.stringify(data));
      }
    });
  });

  ws.on('close', () => {
    clients = clients.filter(c => c !== ws);
    console.log('Client disconnected');
  });
});


server.listen(port, '0.0.0.0', async () => {
    console.log("Słucham");
})

