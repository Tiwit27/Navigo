import con from "../dbConnect.js";

export const getRooms = async (req, res) => {
    try
    {
        const institution_id = req.params.id;
        const [data] = await con.execute("SELECT room_id, name, rooms.beacon_id from rooms INNER JOIN beacons ON rooms.beacon_id = beacons.beacon_id WHERE beacons.institution_id = ?", [institution_id]);
        return res.json({data: data, status: true});
    }
    catch(error)
    {
        return res.json({ status: false, error: error.message });
    }
}

export const changeRoomData = async (req, res) => {
    try
    {
        const roomId = req.params.id;
        const room = req.body;
        if(!room || !room.name || !room.beaconId)
        {
            throw new Error("WRONG_BODY");
        }
        const [beaconIdTest] = await con.execute("SELECT * FROM beacons WHERE beacon_id = ?",[room.beaconId])
        if(beaconIdTest.length == 0)
        {
            throw new Error("WRONG_BEACON_ID");
        }
        const [data] = await con.execute("UPDATE rooms SET name = ?, beacon_id = ? WHERE room_id = ?", [room.name, room.beaconId, roomId]);
        if(data.affectedRows == 0)
        {
            throw new Error("WRONG_ROOM_ID");
        }
        return res.json({data: data.affectedRows, status: true});
    }
    catch(error)
    {
        console.log(error.message);
        return res.json({ status: false, error: error.message });
    }
}

export const deleteRoom = async (req, res) => {
    try
    {
        const roomId = req.params.id;
        const [data] = await con.execute("DELETE FROM rooms WHERE room_id = ?", [roomId]);
        if(data.affectedRows == 0)
        {
            throw new Error("WRONG_ROOM_ID");
        }
        return res.json({data: data.affectedRows, status: true});
    }
    catch(error)
    {
        return res.json({ status: false, error: error.message });
    }
}