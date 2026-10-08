import express from "express"; 
import { changeConnectionData, deleteConnection, getConnections } from "../controlles/connection.js";
import { VerifyToken } from "../middlewares.js";
const router = express.Router();

router.get("/get-connections/:id", VerifyToken, getConnections);
router.patch("/change-connection-data/:id", VerifyToken, changeConnectionData);
router.delete("/delete-connection/:id", VerifyToken, deleteConnection);

export default router;