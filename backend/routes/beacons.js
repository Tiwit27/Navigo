import express from "express"; 
import { getBeacons, changeBeaconData, deleteBeacon, updateBeaconBatteryLevel } from "../controlles/beacon.js";
import { VerifyToken } from "../middlewares.js";
const router = express.Router();

router.get("/get-beacons/:id", VerifyToken, getBeacons);
router.patch("/change-beacon-data/:id", VerifyToken, changeBeaconData);
router.delete("/delete-beacon/:id", VerifyToken, deleteBeacon);
router.patch("/update-beacon-battery-level/:mac", VerifyToken, updateBeaconBatteryLevel);

export default router;