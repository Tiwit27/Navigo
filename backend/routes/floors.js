import express from "express"; 
import { getFloors } from "../controlles/floor.js";
import { VerifyToken } from "../middlewares.js";
const router = express.Router();

router.get("/get-floors/:id", VerifyToken, getFloors);

export default router;