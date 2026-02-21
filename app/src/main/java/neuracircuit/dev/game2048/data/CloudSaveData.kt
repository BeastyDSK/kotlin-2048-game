package neuracircuit.dev.game2048.data

import neuracircuit.dev.game2048.model.Tile
import org.json.JSONArray
import org.json.JSONObject

data class CloudSaveData(
    val grid: List<Tile>,
    val score: Int,
    val highScore: Int,
    val freeUndosLeft: Int
) {
    fun toByteArray(): ByteArray {
        val json = JSONObject()
        json.put("score", score)
        json.put("highScore", highScore)
        json.put("freeUndosLeft", freeUndosLeft)
        
        val gridArray = JSONArray()
        grid.forEach { tile ->
            val tileObj = JSONObject()
            // Saving core standard Tile attributes
            tileObj.put("value", tile.value)
            tileObj.put("x", tile.x)
            tileObj.put("y", tile.y)
            tileObj.put("isNew", tile.isNew)
            gridArray.put(tileObj)
        }
        json.put("grid", gridArray)
        return json.toString().toByteArray(Charsets.UTF_8)
    }

    companion object {
        fun fromByteArray(bytes: ByteArray): CloudSaveData? {
            return try {
                val json = JSONObject(String(bytes, Charsets.UTF_8))
                val score = json.getInt("score")
                val highScore = json.getInt("highScore")
                val freeUndosLeft = json.optInt("freeUndosLeft", 3)
                
                val gridArray = json.getJSONArray("grid")
                val grid = mutableListOf<Tile>()
                for (i in 0 until gridArray.length()) {
                    val tObj = gridArray.getJSONObject(i)
                    grid.add(
                        Tile(
                            value = tObj.getInt("value"),
                            x = tObj.getInt("x"),
                            y = tObj.getInt("y"),
                            isNew = tObj.getBoolean("isNew")
                        )
                    )
                }
                CloudSaveData(grid, score, highScore, freeUndosLeft)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
