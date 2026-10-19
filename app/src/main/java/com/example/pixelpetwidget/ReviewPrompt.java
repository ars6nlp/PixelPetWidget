package com.example.pixelpetwidget;

import android.app.Activity;

import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

/**
 * Google Play's in-app rating dialog, asked once after a few days in a row. Play decides
 * whether the dialog actually appears (it is rate-limited), so nothing depends on it.
 */
final class ReviewPrompt {

    private ReviewPrompt() {}

    static void show(Activity activity) {
        ReviewManager manager = ReviewManagerFactory.create(activity);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            ReviewInfo info = task.getResult();
            manager.launchReviewFlow(activity, info);
        });
    }
}
