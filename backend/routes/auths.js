import express from "express"; 
import { autoLogin, login, register } from "../controlles/auth.js";
import { VerifySecretKey, VerifyToken } from "../middlewares.js";
const router = express.Router();

router.post("/register", VerifySecretKey, register);
router.post("/login", VerifySecretKey, login);
router.get("/auto-login", VerifyToken, autoLogin);

export default router;