package com.gymable.data

import com.gymable.data.local.entity.Exercise
import com.gymable.data.local.entity.Gym
import com.gymable.data.local.entity.MuscleGroup
import com.gymable.data.local.entity.Record
import com.gymable.data.local.entity.WorkoutSession
import com.gymable.data.local.entity.WorkoutSet
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymDataImporter @Inject constructor() {

    fun parse(json: String): ExportData {
        if (json.startsWith("%PDF")) {
            throw IllegalArgumentException("Wrong file selected. Please choose the .gymdata backup file, not the PDF report.")
        }
        val root = JSONObject(json)
        if (root.optInt("version", 0) != 1) {
            throw IllegalArgumentException("Unsupported backup version")
        }
        val exportDate = root.optLong("exportDate", System.currentTimeMillis())
        val gymsArr = root.getJSONArray("gyms")

        return ExportData(
            exportDateMs = exportDate,
            gyms = (0 until gymsArr.length()).map { gi ->
                val gymObj = gymsArr.getJSONObject(gi)
                val mgsArr = gymObj.getJSONArray("muscleGroups")
                GymExport(
                    gym = Gym(name = gymObj.getString("name")),
                    muscleGroups = (0 until mgsArr.length()).map { mi ->
                        val mgObj = mgsArr.getJSONObject(mi)
                        val exsArr = mgObj.getJSONArray("exercises")
                        MuscleGroupExport(
                            muscleGroup = MuscleGroup(name = mgObj.getString("name"), gymId = 0),
                            exercises = (0 until exsArr.length()).map { ei ->
                                parseExercise(exsArr.getJSONObject(ei))
                            }
                        )
                    }
                )
            }
        )
    }

    private fun parseExercise(exObj: JSONObject): ExerciseExport {
        val record = if (exObj.has("record")) {
            val recObj = exObj.getJSONObject("record")
            Record(
                exerciseId = 0,
                maxWeight = recObj.getDouble("maxWeight").toFloat(),
                maxReps = recObj.getInt("maxReps")
            )
        } else null

        val sessionsArr = exObj.optJSONArray("sessions") ?: JSONArray()
        val sessions = (0 until sessionsArr.length()).map { si ->
            val sesObj = sessionsArr.getJSONObject(si)
            val setsArr = sesObj.optJSONArray("sets") ?: JSONArray()
            SessionWithSets(
                session = WorkoutSession(
                    exerciseId = 0,
                    date = sesObj.getLong("date"),
                    rating = sesObj.optInt("rating", 5)
                ),
                sets = (0 until setsArr.length()).mapIndexed { i, _ ->
                    val setObj = setsArr.getJSONObject(i)
                    WorkoutSet(
                        sessionId = 0,
                        setNumber = i + 1,
                        reps = setObj.getInt("reps"),
                        weight = setObj.getDouble("weight").toFloat()
                    )
                }
            )
        }

        return ExerciseExport(
            exercise = Exercise(name = exObj.getString("name"), muscleGroupId = 0, isUnilateral = exObj.optBoolean("isUnilateral", false)),
            record = record,
            sessions = sessions
        )
    }
}
