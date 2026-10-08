import con from "../dbConnect.js";

export const getConnections = async (req, res) => {
    try
    {
        const institution_id = req.params.id;
        const [data] = await con.execute("SELECT connection_id, bottom_x, bottom_y, bottom_floor, top_x, top_y, top_floor FROM connections WHERE institution_id = ?", [institution_id]);
        return res.json({data: data, status: true});
    }
    catch(error)
    {
        console.log(error.message)
        return res.json({ status: false, error: error.message });
    }
}

export const changeConnectionData = async (req, res) => {
    try
    {
        const connection_id = req.params.id;
        const connection = req.body;
        if(!connection || !connection.bottom_x || !connection.bottom_y || !connection.bottom_floor || !connection.top_x || !connection.top_y || !connection.top_floor)
        {
            throw new Error("WRONG_BODY");
        }
        const [data] = await con.execute("UPDATE connections SET bottom_x = ?, bottom_y = ?, bottom_floor = ?, top_x = ?, top_y = ?, top_floor = ? WHERE connection_id = ?", [connection.bottom_x, connection.bottom_y, connection.bottom_floor, connection.top_x, connection.top_y, connection.top_floor, connection_id]);
        if(data.affectedRows == 0)
        {
            throw new Error("WRONG_CONNECTION_ID");
        }
        return res.json({data: data.affectedRows, status: true});
    }
    catch(error)
    {
        return res.json({ status: false, error: error.message });
    }
}

export const deleteConnection = async (req, res) => {
    try
    {
        const connection_id = req.params.id;
        const [data] = await con.execute("DELETE FROM connections WHERE connection_id = ?", [connection_id]);
        if(data.affectedRows == 0)
        {
            throw new Error("WRONG_CONNECTION_ID");
        }
        return res.json({data: data.affectedRows, status: true});
    }
    catch(error)
    {
        return res.json({ status: false, error: error.message });
    }
}