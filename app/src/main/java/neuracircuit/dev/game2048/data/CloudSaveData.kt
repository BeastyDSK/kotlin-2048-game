package neuracircuit.dev.game2048.data

import neuracircuit.dev.game2048.model.Tile
import org.json.JSONArray
import org.json.JSONObject
import neuracircuit.dev.game2048.viewmodel.Snapshot

data class CloudSaveData(
    val grid: List<Tile>,
    val score: Int,
    val highScore: Int,
    val freeUndosLeft: Int,
    val history: List<Snapshot> = emptyList() // NEW: Include history in the payload
) {
    fun toByteArray(): ByteArray {
        val json = JSONObject()
        json.put("score", score)
        json.put("highScore", highScore)
        json.put("freeUndosLeft", freeUndosLeft)
        
        // Serialize current grid
        json.put("grid", gridToJsonArray(grid))

        // Serialize history queue
        val historyArray = JSONArray()
        history.forEach { snapshot ->
            val snapObj = JSONObject()
            snapObj.put("score", snapshot.score)
            snapObj.put("tiles", gridToJsonArray(snapshot.tiles))
            historyArray.put(snapObj)
        }
        json.put("history", historyArray)

        return json.toString().toByteArray(Charsets.UTF_8)
    }

    private fun gridToJsonArray(grid: List<Tile>): JSONArray {
        val gridArray = JSONArray()
        grid.forEach { tile ->
            val tileObj = JSONObject()
            tileObj.put("value", tile.value)
            tileObj.put("x", tile.x)
            tileObj.put("y", tile.y)
            tileObj.put("isNew", tile.isNew)
            gridArray.put(tileObj)
        }
        return gridArray
    }

    companion object {
        fun fromByteArray(bytes: ByteArray, onError: ((Exception) -> Unit)? = null): CloudSaveData? {
            return try {
                val json = JSONObject(String(bytes, Charsets.UTF_8))
                val score = json.getInt("score")
                val highScore = json.getInt("highScore")
                val freeUndosLeft = json.optInt("freeUndosLeft", 3)
                
                val grid = parseGrid(json.getJSONArray("grid"))
                
                // Parse the history array if it exists (backward compatibility)
                val history = mutableListOf<Snapshot>()
                if (json.has("history")) {
                    val historyArray = json.getJSONArray("history")
                    for (i in 0 until historyArray.length()) {
                        val snapObj = historyArray.getJSONObject(i)
                        val snapScore = snapObj.getInt("score")
                        val snapGrid = parseGrid(snapObj.getJSONArray("tiles"))
                        history.add(Snapshot(snapGrid, snapScore))
                    }
                }

                CloudSaveData(grid, score, highScore, freeUndosLeft, history)
            } catch (e: Exception) {
                onError?.invoke(e)
                null
            }
        }

        private fun parseGrid(gridArray: JSONArray): List<Tile> {
            val grid = mutableListOf<Tile>()
            for (i in 0 until gridArray.length()) {
                val tObj = gridArray.getJSONObject(i)
                grid.add(
                    Tile(
                        value = tObj.getInt("value"),
                        x = tObj.getInt("x"),
                        y = tObj.getInt("y"),
                        isNew = tObj.optBoolean("isNew", false)
                    )
                )
            }
            return grid
        }
    }
}
