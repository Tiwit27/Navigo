import con from "../dbConnect.js";
import path from 'path';
import fs from "fs/promises"
import { existsSync } from 'fs'; 
import { __dirname } from "../dirnameDeclaration.js";

export const getInstitution = async (req, res) => {
    try
    {
        const institution_id = req.params.id;
        const [data] = await con.execute("SELECT name, short_name FROM institutions WHERE institution_id = ?", [institution_id]);
        if(data.length == 0)
        {
            throw new Error("INSTITUTION_DOESNT_EXIST");
        }
        return res.json({data: data[0], status: true});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const getInstitutionLogo = async (req, res) => {
    try
    {
        const institution_id = req.params.id;
        const [data] = await con.execute("SELECT have_logo FROM institutions WHERE institution_id = ?",[institution_id]);
        if(data.length == 0)
        {
            throw new Error("WRONG_INSTITUTION_ID");
        }
        if(data[0].have_logo == false)
        {
            return res.json({ data: null, status: true });
        }
        const logoPath = path.join(__dirname, "files", "institutions_logos", `${institution_id}.png`);
        console.log(logoPath);
        if(!existsSync(logoPath))
        {
            return res.json({ data: null, status: true });
        }
        res.setHeader("Content-Type", "image/png");
        const file = await fs.readFile(logoPath);
        return res.send(file);
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const getInstitutionId = async (req, res) => {
    try
    {
        const mac = req.params.mac;
        console.log(req.params);
        const [data] = await con.execute("SELECT institution_id FROM beacons WHERE mac = ?",[mac]);
        if(data.length == 0)
        {
            throw new Error("WRONG_MAC_ADDRESS");
        }
        return res.json({data: data[0].institution_id, status: true});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const changeInstitutionData = async (req, res) => {
    try
    {
        const institution_id = req.params.id;
        const file = req.file;
        console.log(file);
        if(!file)
        {
            const [temp] = await con.execute("SELECT have_logo FROM institutions WHERE institution_id = ?",[institution_id]);
            if(temp[0].have_logo == 1)
            {
                await fs.unlink(`files/institutions_logos/${req.params.id}.png`);
            }
            const [data] = await con.execute("UPDATE institutions SET name = ?, short_name = ?, have_logo = 0 WHERE institution_id = ?",[req.body.name, req.body.short_name, institution_id]);
            return res.json({data: "LOGO USUNIĘTE", status: true});
        }
        const [data] = await con.execute("UPDATE institutions SET name = ?, short_name = ?, have_logo = 1 WHERE institution_id = ?",[req.body.name, req.body.short_name, institution_id]);
        return res.json({data: "PLIK ZAPISANY", status: true});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}