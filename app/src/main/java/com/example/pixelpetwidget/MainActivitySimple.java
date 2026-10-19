package com.example.pixelpetwidget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.Locale;

import com.example.pixelpetwidget.sound.SoundManager;
import com.example.pixelpetwidget.ui.pixel.PixelButton;
import com.example.pixelpetwidget.ui.pixel.PixelDialog;
import com.example.pixelpetwidget.ui.pixel.PixelShape;
import com.example.pixelpetwidget.ui.theme.PixelTheme;

import androidx.annotation.NonNull;

/**
 * Главный экран: превью выбранного питомца, «Добавить на экран», кормление, выбор питомца и фона,
 * гардероб, подарок дня. Настройки — страница поверх него (SettingsPage), а не отдельный экран:
 * так возврат из настроек не считается новым запуском для межстраничной рекламы.
 */
public class MainActivitySimple extends AppCompatActivity implements SettingsPage.Host {

    private static final String LANG_PREFS = "PixelPetLangPrefs";
    private static final String LANG_KEY = "selected_language";

    // Pet and background chosen here; WidgetConfigActivity applies them to new widgets
    static final String PET_PREFS = "PixelPetPrefs";
    static final String KEY_SELECTED_PET = "selected_pet";
    static final String KEY_SELECTED_BG = "selected_bg";
    private static final String KEY_SETTINGS_OPEN = "settings_open";

    // Picker order; the keys are what WidgetConfigActivity reads back via petTypeFromName()
    private static final PetCards.Option[] PETS = {
        new PetCards.Option("panda", R.string.pet_panda_desc),
        new PetCards.Option("hamster", R.string.pet_hamster_desc),
        new PetCards.Option("capybara", R.string.pet_capybara_desc),
        new PetCards.Option("parrot", R.string.pet_parrot_desc),
        new PetCards.Option("puppy", R.string.pet_puppy_desc),
        new PetCards.Option("kitten", R.string.pet_kitten_desc),
        new PetCards.Option("aquarium", R.string.pet_aquarium_desc),
        new PetCards.Option("cat", R.string.pet_cat_desc),
        new PetCards.Option("dog", R.string.pet_dog_desc),
        new PetCards.Option("rabbit", R.string.pet_rabbit_desc),
    };

    /** Opens the screen on this pet type (from the widget's need bubble). */
    static final String EXTRA_OPEN_PET = "com.example.pixelpetwidget.extra.OPEN_PET";
    // Сколько играет реакция питомца (ест, радуется) и держится её подпись
    private static final long REACTION_MS = 2600;
    private static final int REVIEW_AFTER_STREAK = 3;
    // Превью той же формы, что сцена виджета: высота = ширина × PREVIEW_RATIO
    private static final float PREVIEW_RATIO = 0.62f;
    // Доля высоты превью, которую занимает питомец обычного размера
    private static final float PREVIEW_PET_RATIO = 0.62f;

    private String selectedPet = "cat";
    private int selectedBg = 1;
    private PetCards petCards;
    private BackgroundCards bgCards;
    private View previewCard;
    private ImageView previewBackground;
    private ImageView previewPet;
    private TextView previewHint;
    private View carePanel;
    private View foodBar;
    private View joyBar;
    private PixelButton feedButton;
    private View mainPage;
    private View settingsPage;
    private DailyGiftCard giftCard;
    private WardrobeCard wardrobeCard;
    private SettingsPage settings;
    private OnBackPressedCallback closeSettingsOnBack;
    // Widgets on the home screen when a pin was requested; more on return means it was placed
    private int widgetsBeforePin = -1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    // Пока реакция не закончилась, подпись под превью не перезаписывается
    private boolean reacting;
    private final Runnable backToCalm = () -> {
        reacting = false;
        renderPreview();
    };
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
        new ActivityResultContracts.RequestPermission(), granted -> settings.onNotificationPermission(granted));

    /** The banner and the interstitial, after consent (Ads). */
    private Ads ads;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(withAppLanguage(newBase));
    }

    /** The context in the language chosen in the app (Android 13+ applies it by itself). */
    static Context withAppLanguage(Context base) {
        String lang = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ? base.getSharedPreferences(LANG_PREFS, MODE_PRIVATE).getString(LANG_KEY, null)
            : null;
        return lang != null ? updateLocale(base, lang) : base;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_simple);
        applyThemeColors();
        applySystemBarInsets();

        // Ads: consent first where it is needed, then the banner and the interstitial
        ads = new Ads(this, findViewById(R.id.adView));
        ads.start();

        mainPage = findViewById(R.id.main_page);
        settingsPage = findViewById(R.id.settings_page);
        limitWidth(findViewById(R.id.main_content));
        limitWidth(findViewById(R.id.settings_content));
        limitWidth(findViewById(R.id.main_top_bar));
        limitWidth(findViewById(R.id.settings_top_bar));

        // Верхняя панель
        ImageView logo = findViewById(R.id.logo);
        logo.setImageDrawable(PixelTheme.icon(this, R.drawable.ic_px_logo, 0));
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            // Только debug-сборка: долгое нажатие на логотип открывает экран со всеми компонентами
            logo.setOnLongClickListener(v -> {
                startActivity(new Intent().setClassName(getPackageName(),
                    "com.example.pixelpetwidget.UiGalleryActivity"));
                return true;
            });
        }
        findViewById(R.id.settings_button).setOnClickListener(v -> showSettings());

        // Превью
        previewCard = findViewById(R.id.preview_card);
        previewBackground = findViewById(R.id.preview_background);
        previewBackground.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t != ob - ot || r - l != or - ol) {
                placeBackdrop();
            }
        });
        previewPet = findViewById(R.id.preview_pet);
        previewHint = findViewById(R.id.preview_hint);
        PixelTheme.Colors colors = PixelTheme.colors(this);
        int stroke = PixelTheme.stroke(this);
        previewCard.setBackground(new PixelShape(colors.surface, colors.line, stroke));
        previewCard.setPadding(stroke, stroke, stroke, stroke);
        previewCard.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (right - left != oldRight - oldLeft) {
                previewCard.post(this::fitPreview);
            }
        });
        previewPet.setOnClickListener(v -> stroke());

        petCards = new PetCards(this, (GridLayout) findViewById(R.id.pet_grid), PETS, this::onPetTapped);

        // Cards 1–3: the painted pictures; then the pixel-art ones (bg_type 4 on)
        bgCards = new BackgroundCards(findViewById(R.id.bg_cards), background -> {
            selectedBg = background;
            renderSelection();
        });

        findViewById(R.id.add_widget_button).setOnClickListener(v -> addWidget());

        // Забота: кормление, поглаживание, подарок дня, гардероб, настройки
        carePanel = findViewById(R.id.care_panel);
        foodBar = findViewById(R.id.care_food_bar);
        joyBar = findViewById(R.id.care_joy_bar);
        feedButton = findViewById(R.id.feed_button);
        feedButton.setClickSound(false);              // кормление звучит голосом питомца
        feedButton.setOnClickListener(v -> feed());
        giftCard = new DailyGiftCard(this, this::onGiftClaimed);
        wardrobeCard = new WardrobeCard(this, this::onWardrobeChanged);
        settings = new SettingsPage(this, this);
        giftCard.bind();

        closeSettingsOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                closeSettings();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeSettingsOnBack);
        findViewById(R.id.settings_back).setOnClickListener(v -> closeSettings());

        // Restore the selection: unsaved changes after rotation, otherwise the last saved choice
        SharedPreferences petPrefs = getSharedPreferences(PET_PREFS, MODE_PRIVATE);
        selectedPet = petPrefs.getString(KEY_SELECTED_PET, "cat");
        selectedBg = petPrefs.getInt(KEY_SELECTED_BG, 1);
        if (savedInstanceState != null) {
            selectedPet = savedInstanceState.getString(KEY_SELECTED_PET, selectedPet);
            selectedBg = savedInstanceState.getInt(KEY_SELECTED_BG, selectedBg);
        }
        selectedPet = optionFor(selectedPet).key;
        openPetFrom(getIntent());
        renderSelection();

        // Back on the settings after a language change recreated the screen
        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_SETTINGS_OPEN)) {
            showSettings();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_PET, selectedPet);
        outState.putInt(KEY_SELECTED_BG, selectedBg);
        outState.putBoolean(KEY_SETTINGS_OPEN, settingsPage.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (openPetFrom(intent)) {
            if (settingsPage.getVisibility() == View.VISIBLE) {
                closeSettings();
            }
            renderSelection();
        }
    }

    /** Selects the pet whose need bubble was tapped on the widget. */
    private boolean openPetFrom(Intent intent) {
        if (intent == null || !intent.hasExtra(EXTRA_OPEN_PET)) {
            return false;
        }
        int petType = intent.getIntExtra(EXTRA_OPEN_PET, PixelCatWidget.PET_CAT);
        intent.removeExtra(EXTRA_OPEN_PET);
        for (PetCards.Option pet : PETS) {
            if (pet.spec().petType == petType) {
                selectedPet = pet.key;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        ads.pause();
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        reacting = false;
        petCards.stop();
        if (previewPet.getDrawable() instanceof AnimationDrawable) {
            ((AnimationDrawable) previewPet.getDrawable()).stop();
        }
    }

    @Override
    protected void onDestroy() {
        ads.destroy();
        super.onDestroy();
        if (isFinishing()) {
            SoundManager.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The day, the pets on the home screen or their mood may have changed meanwhile
        giftCard.bind();
        renderPreview();
        petCards.start(!animationsOff());
        checkPinnedWidget();
        ads.resume();
    }

    /** Фон окна и цвет значков системных панелей — из PixelTheme. */
    private void applyThemeColors() {
        PixelTheme.Colors c = PixelTheme.colors(this);
        findViewById(R.id.root_layout).setBackgroundColor(c.background);
        getWindow().getDecorView().setBackgroundColor(c.background);
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(!PixelTheme.isDark(this));
        bars.setAppearanceLightNavigationBars(!PixelTheme.isDark(this));
    }

    // Since Android 15 the window draws behind the status and navigation bars
    private void applySystemBarInsets() {
        View root = findViewById(R.id.root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** На широком экране колонка не шире px_max_content_width и стоит по центру. */
    private void limitWidth(View content) {
        int minPad = PixelTheme.px(this, R.dimen.px_screen_padding);
        int maxWidth = PixelTheme.px(this, R.dimen.px_max_content_width);
        content.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int pad = Math.max(minPad, (r - l - maxWidth) / 2);
            if (v.getPaddingLeft() != pad) {
                v.post(() -> v.setPadding(pad, v.getPaddingTop(), pad, v.getPaddingBottom()));
            }
        });
    }

    /** Системная настройка «Убрать анимации». */
    private boolean animationsOff() {
        return Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
    }

    // --- Settings -----------------------------------------------------------------------------

    private void showSettings() {
        settings.bind();
        settingsPage.setVisibility(View.VISIBLE);
        mainPage.setVisibility(View.INVISIBLE);
        closeSettingsOnBack.setEnabled(true);
        findViewById(R.id.settings_back).requestFocus();
    }

    private void closeSettings() {
        closeSettingsOnBack.setEnabled(false);
        mainPage.setVisibility(View.VISIBLE);
        settingsPage.setVisibility(View.GONE);
        findViewById(R.id.settings_button).requestFocus();
        refreshCare();
        // A pause the user made: the moment for an interstitial (Ads decides whether it is due)
        ads.atUserPause();
    }

    @Override
    public boolean adPrivacyOptionsRequired() {
        return ads.privacyOptionsRequired();
    }

    @Override
    public void showAdPrivacyOptions() {
        ads.showPrivacyOptions();
    }

    @Override
    public void requestNotificationPermission() {
        launchNotificationPermission();
    }

    // SettingsPage only asks on Android 13+, where the permission exists
    @SuppressLint("InlinedApi")
    private void launchNotificationPermission() {
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    // Language the screen is shown in: Russian for Russian, English for everything else
    @Override
    public String currentLanguage() {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        return "ru".equals(locale.getLanguage()) ? "ru" : "en";
    }

    @Override
    public void switchLanguage(String lang) {
        if (lang.equals(currentLanguage())) return; // no change needed
        getSharedPreferences(LANG_PREFS, MODE_PRIVATE).edit().putString(LANG_KEY, lang).apply();
        // Recreates the activity in the new language; the settings stay open
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
    }

    private static Context updateLocale(Context context, String lang) {
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(lang));
        return context.createConfigurationContext(config);
    }

    // --- Pet and background ---------------------------------------------------------------

    private void onPetTapped(String key, boolean wasChosen) {
        if (wasChosen) {
            // Повторное нажатие на выбранного питомца: он рад вас видеть
            SoundManager.get(this).pet(currentSpec().petType);
            react(currentSpec(), PixelCatWidget.ANIMATION_HAPPY, null);
            return;
        }
        selectedPet = key;
        renderSelection();
        SoundManager.get(this).chosen(currentSpec().petType);
        // TalkBack announces the tapped card as selected (its state changes under the focus)
        ads.atUserPause();
    }

    private static PetCards.Option optionFor(String key) {
        for (PetCards.Option pet : PETS) {
            if (pet.key.equals(key)) {
                return pet;
            }
        }
        return optionFor("cat");
    }

    /** Отмечает выбранного питомца и фон и обновляет превью. */
    private void renderSelection() {
        petCards.select(selectedPet);
        bgCards.select(selectedBg);
        boolean scene = currentSpec().scene;
        findViewById(R.id.bg_cards).setVisibility(scene ? View.GONE : View.VISIBLE);
        handler.removeCallbacks(backToCalm);
        reacting = false;
        renderPreview();
        wardrobeCard.bind(currentSpec().petType);
    }

    private PetSpec currentSpec() {
        return optionFor(selectedPet).spec();
    }

    // --- Care -------------------------------------------------------------------------------

    private boolean onHomeScreen(PetSpec spec) {
        return CareStore.activePetTypes(this).contains(spec.petType);
    }

    /** Сытость и радость выбранного питомца; кормить можно, только если он на рабочем столе. */
    private void refreshCare() {
        PetSpec spec = currentSpec();
        boolean onHomeScreen = onHomeScreen(spec);
        carePanel.setVisibility(onHomeScreen ? View.VISIBLE : View.GONE);
        feedButton.setVisibility(onHomeScreen ? View.VISIBLE : View.GONE);
        feedButton.setText(getString(R.string.feed_button_count, CareStore.rewards(this).treats));
        if (onHomeScreen) {
            long now = System.currentTimeMillis();
            int food = Math.round(CareStore.food(this, spec.petType, now));
            int joy = Math.round(CareStore.joy(this, spec.petType, now));
            setBar(foodBar, food, getString(R.string.care_food_description, food));
            setBar(joyBar, joy, getString(R.string.care_joy_description, joy));
        }
        previewPet.setContentDescription(getString(R.string.stroke_hint, getString(spec.nameRes)));
        if (!reacting) {
            previewHint.setText(onHomeScreen ? R.string.care_stroke_hint : R.string.care_add_hint);
        }
    }

    /** Полоска 0..100: рамка цвета фона и заливка акцентом слева направо. */
    private void setBar(View bar, int value, String description) {
        PixelTheme.Colors c = PixelTheme.colors(this);
        int stroke = PixelTheme.stroke(this);
        bar.setBackground(new PixelShape(c.background, c.line, stroke));
        Drawable fill = new InsetDrawable(new ClipDrawable(new ColorDrawable(c.accent), Gravity.START,
            ClipDrawable.HORIZONTAL), stroke);
        fill.setLevel(value * 100);
        bar.setForeground(fill);
        bar.setContentDescription(description);
    }

    private void feed() {
        PetSpec spec = currentSpec();
        switch (CareStore.feed(this, spec.petType, System.currentTimeMillis())) {
            case FED:
                SoundManager.get(this).pet(spec.petType);
                react(spec, PixelCatWidget.ANIMATION_EAT, getString(R.string.feed_done));
                careChanged(spec);
                break;
            case ALREADY_FULL:
                SoundManager.get(this).click();
                showReaction(getString(R.string.feed_full));
                break;
            case NO_TREATS:
                SoundManager.get(this).error();
                showReaction(getString(R.string.feed_no_treats));
                break;
        }
    }

    /** Нажатие на питомца в превью — погладить. */
    private void stroke() {
        PetSpec spec = currentSpec();
        boolean onHomeScreen = onHomeScreen(spec);
        boolean counted = onHomeScreen && CareStore.stroke(this, spec.petType, System.currentTimeMillis());
        SoundManager.get(this).pet(spec.petType);
        react(spec, PixelCatWidget.ANIMATION_HAPPY,
            getString(onHomeScreen && !counted ? R.string.stroke_enough : R.string.stroke_done));
        if (counted) {
            careChanged(spec);
        }
    }

    /** Короткая реакция в превью (ест, радуется) с подписью, потом снова спокойный цикл. */
    private void react(PetSpec spec, int animationType, String text) {
        if (spec.hasAnimation(animationType)) {
            startPetAnimation(spec, animationType);
        }
        if (text != null) {
            showReaction(text);
        } else {
            handler.removeCallbacks(backToCalm);
            handler.postDelayed(backToCalm, REACTION_MS);
        }
    }

    /** Подпись под превью на пару секунд. */
    private void showReaction(String text) {
        reacting = true;
        previewHint.setText(text);
        handler.removeCallbacks(backToCalm);
        handler.postDelayed(backToCalm, REACTION_MS);
    }

    private void careChanged(PetSpec spec) {
        refreshCare();
        PixelCatWidget.updateWidgetsOf(this, spec.petType);
        CareScheduler.scheduleCareTick(this);
    }

    private void onGiftClaimed(DailyRewards.Claim claim) {
        SoundManager.get(this).success(currentSpec().petType);
        refreshCare();
        if (claim.unlockedItem != null) {
            wardrobeCard.bind(currentSpec().petType);
            PixelDialog.with(this)
                .title(getString(R.string.wardrobe_new_item, getString(Wardrobe.nameRes(claim.unlockedItem))))
                .show();
        } else if (!PetSettings.reminders(this) && !PetSettings.remindersOffered(this)) {
            offerReminders();
        } else if (claim.streak >= REVIEW_AFTER_STREAK && !PetSettings.reviewAsked(this)) {
            PetSettings.setReviewAsked(this);
            ReviewPrompt.show(this);
        }
    }

    /** Suggests the daily reminder once, right after the first gift. */
    private void offerReminders() {
        PetSettings.setRemindersOffered(this);
        PixelDialog.with(this)
            .title(R.string.reminder_offer_title)
            .message(R.string.reminder_offer_text)
            .positive(R.string.reminder_offer_yes, () -> settings.turnOnReminders())
            .negative(R.string.reminder_offer_no, null)
            .show();
    }

    private void onWardrobeChanged() {
        PetSpec spec = currentSpec();
        react(spec, PixelCatWidget.ANIMATION_HAPPY, null);
        PixelCatWidget.updateWidgetsOf(this, spec.petType);
    }

    // --- Preview ----------------------------------------------------------------------------

    /** Выбранный питомец на выбранном фоне в спокойном цикле. */
    private void renderPreview() {
        PetSpec spec = currentSpec();
        previewCard.setContentDescription(getString(R.string.preview_description, getString(spec.nameRes)));
        if (Backdrop.forType(selectedBg) == null) {
            // A painted picture fills the preview; a pixel-art one is placed in fitPreview
            previewBackground.setScaleType(ImageView.ScaleType.CENTER_CROP);
            previewBackground.setBackground(null);
            previewBackground.setImageResource(backgroundFor(selectedBg));
        }
        previewBackground.setVisibility(spec.scene ? View.GONE : View.VISIBLE);
        startPetAnimation(spec, PetCards.showcase(spec));
        fitPreview();
        refreshCare();
    }

    /** Анимация питомца в превью (с надетой вещью); при «Убрать анимации» — только первый кадр. */
    private void startPetAnimation(PetSpec spec, int animationType) {
        PetSpec.Anim anim = spec.animation(animationType, CareStore.wearing(this, spec.petType));
        if (animationsOff()) {
            previewPet.setImageResource(anim.frames[0]);
            return;
        }
        AnimationDrawable animation = new AnimationDrawable();
        for (int frame : anim.frames) {
            animation.addFrame(ContextCompat.getDrawable(this, frame), anim.intervalMs);
        }
        animation.setOneShot(false);
        previewPet.setImageDrawable(animation);
        animation.start();
    }

    private static int backgroundFor(int background) {
        if (background == 2) return R.drawable.widget_background_2;
        if (background == 3) return R.drawable.widget_background_3;
        return R.drawable.widget_background;
    }

    /** Высота превью по ширине и размер питомца; пиксельные питомцы — только в целом масштабе. */
    private void fitPreview() {
        int width = previewCard.getWidth();
        if (width == 0) {
            return;
        }
        // В повороте экрана превью не выше половины экрана
        int height = Math.min(Math.round(width * PREVIEW_RATIO), getResources().getDisplayMetrics().heightPixels / 2);
        ViewGroup.LayoutParams card = previewCard.getLayoutParams();
        if (card.height != height) {
            card.height = height;
            previewCard.setLayoutParams(card);
        }
        PetSpec spec = currentSpec();
        ViewGroup.LayoutParams params = previewPet.getLayoutParams();
        if (spec.scene) {
            // A scene fills the preview at the largest whole-number scale, as on the widget
            int inner = PixelTheme.px(this, R.dimen.px_gap);
            int scale = Math.max(1, Math.min((width - 2 * inner) / spec.frameWidth,
                (height - 2 * inner) / spec.frameHeight));
            if (params.width != spec.frameWidth * scale || params.height != spec.frameHeight * scale) {
                params.width = spec.frameWidth * scale;
                params.height = spec.frameHeight * scale;
                previewPet.setLayoutParams(params);
            }
            return;
        }
        // Big and small pets keep their proportions to each other, as on the widget
        int size = Math.round(height * PREVIEW_PET_RATIO * spec.sizeRatio / PetSpec.DEFAULT_SIZE_RATIO);
        if (spec.pixelGrid > 0) {
            size = PixelCatWidget.pixelSnappedSize(size, spec.pixelGrid);
            while (size > height - PixelTheme.px(this, R.dimen.px_section_gap) && size > spec.pixelGrid) {
                size -= spec.pixelGrid;
            }
        }
        if (params.width != size || params.height != size) {
            params.width = size;
            params.height = size;
            previewPet.setLayoutParams(params);
        }
        previewScale = Math.max(1, Math.round(size / (float) PetSpec.PIXEL_GRID));
        placeBackdrop();
    }

    /** Pixel scale of the pet in the preview, for a pixel-art background behind it. */
    private int previewScale = 1;

    /** As on the widget: a pixel-art background at the pet's pixel scale, its ground under the pet's feet. */
    private void placeBackdrop() {
        Backdrop backdrop = Backdrop.forType(selectedBg);
        if (backdrop != null && !currentSpec().scene && previewBackground.getHeight() > 0) {
            int feet = PixelTheme.px(this, R.dimen.px_gap) + PixelCatWidget.FEET_ROWS * previewScale;
            BackgroundCards.showOnGround(previewBackground, backdrop, previewScale, feet);
        }
    }

    // --- Adding a widget --------------------------------------------------------------------

    private int widgetCount() {
        return AppWidgetManager.getInstance(this)
            .getAppWidgetIds(new ComponentName(this, PixelCatWidget.class)).length;
    }

    /** After a pin request: a cheerful note once the new widget is on the home screen. */
    private void checkPinnedWidget() {
        if (widgetsBeforePin >= 0 && widgetCount() > widgetsBeforePin) {
            widgetsBeforePin = -1;
            SoundManager.get(this).success(currentSpec().petType);
            showReaction(getString(R.string.widget_added));
            refreshCare();
        }
    }

    private void addWidget() {
        int petType = PixelCatWidget.petTypeFromName(selectedPet);

        // Save selected pet and bg as the default for NEW widgets
        getSharedPreferences(PET_PREFS, MODE_PRIVATE).edit()
            .putString(KEY_SELECTED_PET, selectedPet)
            .putInt(KEY_SELECTED_BG, selectedBg)
            .apply();

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, PixelCatWidget.class);
        int widgets = appWidgetManager.getAppWidgetIds(provider).length;
        if (widgets >= WidgetConfigActivity.MAX_WIDGETS) {
            PixelDialog.with(this)
                .title(R.string.max_widgets_title)
                .message(R.string.max_widgets_reached)
                .error()
                .show();
            return;
        }

        // Let the launcher place the widget directly when it supports it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported()) {
            Intent callback = new Intent(this, PixelCatWidget.class);
            callback.setAction(PixelCatWidget.ACTION_WIDGET_PINNED);
            callback.putExtra(PixelCatWidget.EXTRA_PET_TYPE, petType);
            callback.putExtra(PixelCatWidget.EXTRA_BG_TYPE, selectedBg);
            // Mutable: the launcher adds the new widget id to the intent
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent successCallback = PendingIntent.getBroadcast(this, 0, callback, flags);
            if (appWidgetManager.requestPinAppWidget(provider, null, successCallback)) {
                widgetsBeforePin = widgets;
                return;
            }
        }

        showAddWidgetInstructions();
    }

    // Fallback for launchers that can't pin widgets: explain how to add one manually
    private void showAddWidgetInstructions() {
        String petName = getString(currentSpec().nameRes);
        PixelDialog.with(this)
            .title(getString(R.string.dialog_title_format, petName))
            .message(getString(R.string.dialog_message_format, petName))
            .show();
    }
}
