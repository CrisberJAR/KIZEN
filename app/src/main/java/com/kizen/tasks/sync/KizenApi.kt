package com.kizen.tasks.sync

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface KizenApi {
    @GET("api/v3/sync")
    suspend fun getSync(): SyncSnapshotDto

    @PUT("api/v3/sync")
    suspend fun putSync(@Body body: SyncSnapshotDto): SyncSnapshotDto

    @GET("api/v3/tasks/insights")
    suspend fun insights(): InsightSummaryDto

    @POST("api/v3/ai/summary")
    suspend fun aiSummary(): InsightSummaryDto

    @POST("api/v3/alexa/events")
    suspend fun alexaEvent(@Body event: AlexaTaskEvent): AlexaSpeakDto

    @POST("api/v3/alexa/chime")
    suspend fun alexaChime(@Body body: AlexaChimeDto): AlexaChimeResultDto
}
