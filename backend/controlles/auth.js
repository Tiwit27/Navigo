import con from "../dbConnect.js";
import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";

export const register = async (req, res) => {
    try
    {
        const email = req.body.email;
        const password = req.body.password;
        const [temp] = await con.execute('SELECT COUNT(*) as "count" FROM accounts WHERE email = ?', [email]);
        if(temp[0].count != 0)
        {
            throw new Error("EMAIL_ALREADY_EXIST");
        }
        const emailRegex = /^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$/;
        const passwordRegex = /^(?=.*[A-Z]).{8,100}$/;
        if(!emailRegex.test(email))
        {
            throw new Error("INVALID_EMAIL_PATTERN");
        }
        if(!passwordRegex.test(password))
        {
            throw new Error("INVALID_PASSWORD_PATTERN");
        }
        const hashedPassword = await bcrypt.hash(password, 10);
        await con.execute('INSERT INTO accounts(email, password) VALUES (?,?)', [email, hashedPassword]);
        const [data] = await con.execute("SELECT account_id, email, token_version FROM accounts WHERE email = ?", [email]);
        const [institutions] = await con.execute("SELECT institution_id FROM institutions_admins WHERE account_id = ?",[data[0].account_id]);
        data[0].institutions_admin = institutions.map(i => i.institution_id);
        const token = await jwt.sign({account_id: data[0].account_id, token_version: data[0].token_version}, process.env.SECRET_KEY, {expiresIn: "21d"});
        return res.json({data: data[0], status: true, token: token});
    }
    catch(error)
    {
        return res.json({ status: false, error: error.message });
    }
}

export const login = async (req, res) => {
    try
    {
        const email = req.body.email;
        const password = req.body.password;
        const [temp] = await con.execute('SELECT password FROM accounts WHERE email = ?', [email])
        if(temp.length == 0)
        {
            throw new Error("EMAIL_DONT_EXIST");
        }
        const isMatch = await bcrypt.compare(password, temp[0].password)
        if(!isMatch)
        {
            throw new Error("WRONG_PASSWORD")
        }
        const [data] = await con.execute("SELECT account_id, token_version FROM accounts WHERE email = ?", [email]);
        const [institutions] = await con.execute("SELECT institution_id FROM institutions_admins WHERE account_id = ?",[data[0].account_id]);
        data[0].institutions_admin = institutions.map(i => i.institution_id);
        const token = await jwt.sign({account_id: data[0].account_id, token_version: data[0].token_version}, process.env.SECRET_KEY, {expiresIn: "21d"});
        return res.json({data: data[0], status: true, token: token});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const autoLogin = async (req, res) => {
    try
    {
        const [institutions] = await con.execute("SELECT institution_id FROM institutions_admins WHERE account_id = ?",[req.user.account_id]);
        req.user.institutions_admin = institutions.map(i => i.institution_id);
        return res.json({data: req.user, status: true, token: req.token});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}