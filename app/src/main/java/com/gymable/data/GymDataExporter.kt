package com.gymable.data

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GymDataExporter @Inject constructor() {

    fun generate(data: ExportData, from: Long, until: Long): String =
        JSONObject().apply {
            put("version", 1)
            put("exportDate", data.exportDateMs)
            put("from", from)
            put("until", until)
            put("gyms", JSONArray().apply {
                data.gyms.forEach { gymExport ->
                    put(JSONObject().apply {
                        put("name", gymExport.gym.name)
                        put("muscleGroups", JSONArray().apply {
                            gymExport.muscleGroups.forEach { mgExport ->
                                put(JSONObject().apply {
                                    put("name", mgExport.muscleGroup.name)
                                    put("exercises", JSONArray().apply {
                                        mgExport.exercises.forEach { exExport ->
                                            put(JSONObject().apply {
                                                put("name", exExport.exercise.name)
                                                put("isUnilateral", exExport.exercise.isUnilateral)
                                                exExport.record?.let { rec ->
                                                    put("record", JSONObject().apply {
                                                        put("maxWeight", rec.maxWeight.toDouble())
                                                        put("maxReps", rec.maxReps)
                                                    })
                                                }
                                                put("sessions", JSONArray().apply {
                                                    exExport.sessions.forEach { sws ->
                                                        put(JSONObject().apply {
                                                            put("date", sws.session.date)
                                                            put("rating", sws.session.rating)
                                                            put("sets", JSONArray().apply {
                                                                sws.sets.forEach { set ->
                                                                    put(JSONObject().apply {
                                                                        put("reps", set.reps)
                                                                        put("weight", set.weight.toDouble())
                                                                    })
                                                                }
                                                            })
                                                        })
                                                    }
                                                })
                                            })
                                        }
                                    })
                                })
                            }
                        })
                    })
                }
            })
        }.toString(2)
}
