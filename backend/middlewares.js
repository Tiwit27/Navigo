import jwt from "jsonwebtoken";
import con from "./dbConnect.js";

export const VerifyToken = async (req, res, next) => {
    try
    {
        let token = req.headers["authorization"];
        if(!token)
        {
            throw new Error("EMPTY_TOKEN")
        }
        const payload = await jwt.verify(token, process.env.SECRET_KEY)
        console.log(payload);
        const token_version = payload.token_version;
        const account_id = payload.account_id;
        const [data] = await con.query("SELECT account_id, email, token_version FROM accounts WHERE account_id = ?",[account_id]);
        if(data.length != 1)
        {
            throw new Error("ACCOUNT_DELETED");
        }
        if(token_version != data[0].token_version)
        {
            throw new Error("WRONG_TOKEN_VERSION");
        }
        req.token = await jwt.sign({account_id: account_id, token_version: token_version}, process.env.SECRET_KEY, {expiresIn: "21d"});
        req.user = data[0]
        next();
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const VerifySecretKey = async (req, res, next) => {
    try
    {
        if(req.body.secretKey != process.env.SECRET_KEY)
        {
            throw new Error("WRONG_SECRET_KEY");
        }
        next();
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}