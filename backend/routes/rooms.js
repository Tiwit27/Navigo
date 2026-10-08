import express from "express"; 
import { changeRoomData, deleteRoom, getRooms } from "../controlles/room.js";
import { VerifyToken } from "../middlewares.js";
const router = express.Router();

router.get("/get-rooms/:id", VerifyToken, getRooms);
router.patch("/change-room-data/:id", VerifyToken, changeRoomData);
router.delete("/delete-room/:id", VerifyToken, deleteRoom);

export default router;