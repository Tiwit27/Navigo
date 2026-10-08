import express from "express"; 
import { upload } from "../multerConfig.js";
import { getInstitutionId, getInstitution, getInstitutionLogo, changeInstitutionData } from "../controlles/institution.js";
import { VerifyToken } from "../middlewares.js";
const router = express.Router();

router.get("/get-institution-name/:id", VerifyToken, getInstitution);
router.get("/get-institution-logo/:id", VerifyToken, getInstitutionLogo);
router.get("/get-institution-id/:mac", VerifyToken, getInstitutionId);
router.patch("/change-institution-data/:id", VerifyToken, upload.single("image") , changeInstitutionData);

export default router;