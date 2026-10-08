import con from "../dbConnect.js";

export const getBeacons = async (req, res) => {
    try 
    {
        const institution_id = req.params.id;
        console.log(req.params);
        const [data] = await con.execute("SELECT * FROM beacons WHERE institution_id = ?", [institution_id]);
        return res.json({ data: data, status: true });
    } 
    catch (err) 
    {
        return res.json({ status: false, error: err.message });
    }
}

export const changeBeaconData = async (req, res) => {
    try
    {
        const beacon_id = req.params.id;
        const {x, y, floor} = req.body
        console.log(req.body);
        const [data] = await con.execute("UPDATE beacons SET x = ?, y = ?, floor = ? WHERE beacon_id = ?",[x, y, floor, beacon_id]);
        console.log(data.affectedRows);
        return res.json({data: data.affectedRows, status: true});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const deleteBeacon = async (req, res) => {
    try
    {
        const beacon_id = req.params.id;
        const [data] = await con.execute("DELETE FROM beacons WHERE beacon_id = ?",[beacon_id]);
        return res.json({data: data.affectedRows, status: true});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const updateBeaconBatteryLevel = async (req, res) => {
    try
    {
        const mac = req.params.mac;
        const [data] = await con.execute("UPDATE beacons SET battery_level = ?, battery_level_timestamp = CURRENT_TIMESTAMP WHERE mac = ?",[req.body.batteryLevel, mac]);
        return res.json({data: data.affectedRows, status: true});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}