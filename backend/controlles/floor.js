import con from "../dbConnect.js";

export const getFloors = async (req, res) => {
    try
    {
        const institution_id = req.params.id;
        const [data] = await con.execute("SELECT floor_id, grid, width, height, floor_number FROM floors WHERE institution_id = ?", [institution_id]);
        if(data.length == 0)
        {
            throw new Error("EMPTY_INSTITUTION");
        }
        let result = [];
        for(let index = 0; index < data.length; index++)
        {
            const floor_id = data[index].floor_id
            const height = data[index].height;
            const width = data[index].width;
            const blobData = data[index].grid;
            const floor_number = data[index].floor_number;
            let bitIndex = 0;
            const matrix = [];
            for (let i = 0; i < height; i++) 
            {
                const row = [];
                for (let j = 0; j < width; j++) 
                {
                    const byteIndex = Math.floor(bitIndex / 8);
                    const bitInByte = 7 - (bitIndex % 8);
                    const byte = blobData[byteIndex];
                    const bit = (byte >> bitInByte) & 1;
                    row.push(bit);
                    bitIndex++;
                }
                matrix.push(row);
            }
            result.push({floor_id: floor_id, floor_number: floor_number,grid: matrix});
        }
        return res.json({data: result, status: true});
    } catch(error)
    {
        return res.json({ status: false, error: error.message });
    }
}