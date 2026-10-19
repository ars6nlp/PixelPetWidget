package com.example.pixelpetwidget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * The app's ads (the main screen's banner and an interstitial). Nothing is requested before the
 * user's consent where the law asks for it (Google's User Messaging Platform: GDPR and the like);
 * the interstitial only comes at a pause the user made (leaving the settings, picking another pet),
 * never on its own, and at most once every {@link #INTERSTITIAL_GAP_MS}.
 */
final class Ads {

    private static final String TAG = "Ads";

    /** Shortest time between two interstitials, and from the first start of the app to the first one. */
    static final long INTERSTITIAL_GAP_MS = 5 * 60_000L;

    private static final String INTERSTITIAL_UNIT = "ca-app-pub-1611801616996597/1550790974";

    /**
     * Who the ads are for. These tags are right only while the app's target audience in Play
     * Console has no children (13 and older): then its ads are not child-directed. Whatever the
     * audience, they are rated G (suitable for all audiences).
     */
    private static final RequestConfiguration REQUESTS = new RequestConfiguration.Builder()
        .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)
        .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build();

    private static final String PREFS = "AdPrefs";
    private static final String KEY_LAST_SHOWN = "interstitial_last_shown";
    /** Kept by older versions (an interstitial every third time the screen came back); removed. */
    private static final String OLD_LAUNCH_COUNT = "launchCount";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final ConsentInformation consent;
    @Nullable private Activity activity;
    @Nullable private AdView banner;
    @Nullable private InterstitialAd interstitial;
    private boolean started;
    private boolean loading;
    private boolean showing;

    Ads(Activity activity, AdView banner) {
        this.activity = activity;
        this.banner = banner;
        appContext = activity.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_LAST_SHOWN)) {
            // The first interstitial comes no sooner than INTERSTITIAL_GAP_MS after the first start
            prefs.edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply();
        }
        prefs.edit().remove(OLD_LAUNCH_COUNT).apply();
        consent = UserMessagingPlatform.getConsentInformation(appContext);
    }

    /**
     * Asks for consent where it is needed (the form shows only then), and starts the ads once they
     * may be requested; with consent given before, they start at once.
     */
    void start() {
        Activity host = activity;
        if (host == null) {
            return;
        }
        ConsentRequestParameters parameters = new ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build();
        consent.requestConsentInfoUpdate(host, parameters,
            () -> {
                Activity current = activity;
                if (current == null) {
                    return;
                }
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(current, formError -> {
                    if (formError != null) {
                        Log.w(TAG, "Consent form: " + formError.getMessage());
                    }
                    startAds();
                });
            },
            requestError -> {
                Log.w(TAG, "Consent info: " + requestError.getMessage());
                startAds();
            });
        // Consent from an earlier start: no need to wait for the update
        startAds();
    }

    /** Whether the user must be able to change their consent (the settings show a button for it). */
    boolean privacyOptionsRequired() {
        return consent.getPrivacyOptionsRequirementStatus()
            == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    /** The consent form again, to change the choice. */
    void showPrivacyOptions() {
        Activity host = activity;
        if (host != null) {
            UserMessagingPlatform.showPrivacyOptionsForm(host, formError -> {
                if (formError != null) {
                    Log.w(TAG, "Privacy options: " + formError.getMessage());
                }
            });
        }
    }

    private void startAds() {
        if (started || activity == null || !consent.canRequestAds()) {
            return;
        }
        started = true;
        MobileAds.setRequestConfiguration(REQUESTS);
        MobileAds.initialize(appContext, status -> { });
        if (banner != null) {
            banner.loadAd(new AdRequest.Builder().build());
        }
        loadInterstitial();
    }

    private void loadInterstitial() {
        if (!started || loading || interstitial != null || activity == null) {
            return;
        }
        loading = true;
        InterstitialAd.load(appContext, INTERSTITIAL_UNIT, new AdRequest.Builder().build(),
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd ad) {
                    loading = false;
                    if (activity == null) {
                        return;
                    }
                    // Kept until the user's next pause; never shown just because it has loaded
                    interstitial = ad;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError error) {
                    loading = false;
                }
            });
    }

    /**
     * After the user's own step to somewhere else (leaving the settings, picking another pet): the
     * interstitial, if one is ready and the last one was long enough ago.
     */
    void atUserPause() {
        Activity host = activity;
        InterstitialAd ad = interstitial;
        long now = System.currentTimeMillis();
        if (host == null || ad == null || showing || now - prefs.getLong(KEY_LAST_SHOWN, 0) < INTERSTITIAL_GAP_MS) {
            return;
        }
        interstitial = null;
        showing = true;
        prefs.edit().putLong(KEY_LAST_SHOWN, now).apply();
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                showing = false;
                loadInterstitial();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                showing = false;
                loadInterstitial();
            }
        });
        ad.show(host);
    }

    void resume() {
        if (banner != null) {
            banner.resume();
        }
    }

    void pause() {
        if (banner != null) {
            banner.pause();
        }
    }

    /** The screen is gone: nothing here keeps it any longer. */
    void destroy() {
        if (banner != null) {
            banner.destroy();
            banner = null;
        }
        if (interstitial != null) {
            interstitial.setFullScreenContentCallback(null);
            interstitial = null;
        }
        activity = null;
    }
}
