package com.example.pixelpetwidget;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

/** Карточка «Подарок дня»: серия дней, лакомства и кнопка «Забрать»; после — «Новый подарок завтра». */
final class DailyGiftCard {

    interface Listener {
        void onClaimed(DailyRewards.Claim claim);
    }

    private final Activity activity;
    private final Listener listener;
    private final TextView status;
    private final TextView result;
    private final TextView claimButton;
    private final View waiting;

    DailyGiftCard(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        status = activity.findViewById(R.id.gift_status);
        result = activity.findViewById(R.id.gift_result);
        claimButton = activity.findViewById(R.id.gift_claim_button);
        waiting = activity.findViewById(R.id.gift_waiting);
        claimButton.setOnClickListener(v -> claim());
    }

    void bind() {
        DailyRewards.State state = CareStore.rewards(activity);
        long today = DayClock.today();
        boolean canClaim = DailyRewards.canClaim(state, today);
        int streak = canClaim ? DailyRewards.currentStreak(state, today) : state.streak;
        String text = activity.getString(R.string.gift_status, streak, state.treats);
        if (state.freezes > 0) {
            text += "\n" + activity.getString(R.string.gift_freezes_description, state.freezes);
        }
        status.setText(text);
        claimButton.setText(activity.getString(R.string.gift_claim, DailyRewards.DAILY_TREATS));
        claimButton.setVisibility(canClaim ? View.VISIBLE : View.GONE);
        waiting.setVisibility(canClaim ? View.GONE : View.VISIBLE);
    }

    private void claim() {
        DailyRewards.State state = CareStore.rewards(activity);
        long today = DayClock.today();
        if (!DailyRewards.canClaim(state, today)) {
            bind();
            return;
        }
        DailyRewards.Claim claim = DailyRewards.claim(state, today);
        CareStore.save(activity, state);

        StringBuilder text = new StringBuilder(
            activity.getString(R.string.gift_received, claim.treats + claim.bonusTreats));
        if (claim.welcomeBack) text.append('\n').append(activity.getString(R.string.gift_welcome_back));
        if (claim.freezesUsed > 0) text.append('\n').append(activity.getString(R.string.gift_freeze_used));
        if (claim.freezeEarned) text.append('\n').append(activity.getString(R.string.gift_freeze_earned));
        if (claim.streakReset) text.append('\n').append(activity.getString(R.string.gift_streak_reset));
        result.setText(text);
        result.setVisibility(View.VISIBLE);

        bind();
        listener.onClaimed(claim);
    }
}
