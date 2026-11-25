package com.gymable.data.repository

import android.app.Activity
import android.content.Context
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val reviewManager = ReviewManagerFactory.create(context)

    suspend fun launchReviewFlow(activity: Activity) {
        try {
            val reviewInfo = reviewManager.requestReview()
            reviewManager.launchReview(activity, reviewInfo)
        } catch (e: Exception) {
            // Play may refuse (no Play Store, quota exhausted, etc.) - review is a nice-to-have, never crash for it
        }
    }
}
