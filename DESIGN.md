# Дизайн приложения Pixel Pet

Коротко о том, как устроены экраны приложения и как их менять. Виджет и питомцы живут отдельно
(см. `tools/sprite_gen/`) и эту тему не используют.

## Где что лежит

| Что | Где |
|---|---|
| Цвета светлой и тёмной темы, шрифт | `app/src/main/java/com/example/pixelpetwidget/ui/theme/PixelTheme.java` |
| Все размеры: обводка, отступы, текст, картинки | `app/src/main/res/values/dimens.xml` (имена `px_*`) |
| Компоненты | `app/src/main/java/com/example/pixelpetwidget/ui/pixel/` |
| Шрифт Tiny5 | `app/src/main/res/font/tiny5_regular.ttf`, лицензия — `licenses/Tiny5-OFL.txt` |
| Иконки (4 штуки) | `app/src/main/res/drawable-nodpi/ic_px_*.png` |
| Звуки | `app/src/main/res/raw/*.wav`, список — в `sound/SoundManager.java` |
| Главный экран | `res/layout/activity_main_simple.xml` + `MainActivitySimple.java` |
| Настройки | `res/layout/page_settings.xml` + `SettingsPage.java` |
| Карточки питомца и вещи | `res/layout/item_pet_card.xml`, `res/layout/item_wardrobe.xml` |
| Тексты | `res/values/strings.xml` (английский) и `res/values-ru/strings.xml` (русский) |

Тёмная тема включается вместе с тёмной темой телефона.

## Как поменять

- **Цвет.** Откройте `PixelTheme.java`: там два набора `LIGHT` и `DARK` по 7 цветов — фон,
  поверхность (карточки и кнопки), текст, второстепенный текст, акцент, обводка, ошибка.
  Поменяйте hex — поменяется везде. Текст на акценте и затемнённые нажатые состояния считаются
  из этих же цветов. Проверьте контраст: текст к фону не меньше 4.5:1.
- **Шрифт.** Положите TTF в `res/font/` и поменяйте `PixelTheme.FONT`. Округление размера до
  кратного 8 px (`FONT_GRID_PX`) нужно только для Tiny5 — для другого шрифта поставьте 1.
- **Обводка.** `px_stroke` в `dimens.xml`. Это же размер «пикселя» иконок и ступенька срезанного угла.
- **Размер кнопки.** Наименьшая высота и ширина — `px_touch` (не меньше 48dp, чтобы попадать
  пальцем), внутренние отступы — `px_card_padding` и `px_gap`.
- **Размер текста.** `px_text_title`, `px_text_body`, `px_text_hint` в `dimens.xml`.
- **Иконка.** Нарисуйте PNG в пиксельном редакторе (один пиксель картинки = один «пиксель»
  интерфейса, рисуйте белым — иконка перекрашивается в цвет текста), положите в
  `res/drawable-nodpi/ic_px_имя.png` и укажите в кнопке: `app:pixelIcon="@drawable/ic_px_имя"`.

## Компоненты

| Компонент | Что это | Главное |
|---|---|---|
| `PixelButton` | Кнопка | `app:pixelVariant="primary"` или `"secondary"`, `app:pixelIcon`; без текста — кнопка-иконка (нужен `contentDescription`) |
| `PixelCard` | Карточка (LinearLayout в рамке) | С `OnClickListener` становится кнопкой; `setSelected(true)` заливает акцентом |
| `PixelSwitch` | Переключатель | Обычный `CompoundButton`: `setChecked`, `setOnCheckedChangeListener` |
| `PixelSlider` | Слайдер | `setMax`, `setProgress`, `setListener` |
| `PixelDialog` | Диалог | `PixelDialog.with(ctx).title(..).message(..).positive(..).negative(..).show()` |
| `PixelLabel` | Текст | `app:pixelText="title"`, `"body"` или `"hint"` |

Все они рисуют фон через `PixelShape`: плоская заливка, обводка, срезанные углы.

### Новый компонент
1. Создайте класс в `ui/pixel/`, унаследуйте от ближайшего стандартного View.
2. Цвета берите из `PixelTheme.colors(getContext())`, размеры — `PixelTheme.px(context, R.dimen.px_...)`,
   рамку — `new PixelShape(fill, line, PixelTheme.stroke(context))`.
3. Сверху напишите комментарий: что делает компонент и что можно менять.
4. Добавьте его на debug-экран (`app/src/debug/.../UiGalleryActivity.java`).

### Новый экран или раздел
Экранов два, и оба живут в одной Activity, чтобы не трогать счётчик межстраничной рекламы.
Раздел на главном экране добавляется в `activity_main_simple.xml` из готовых компонентов
(заголовок — `PixelLabel` с `pixelText="title"`, содержимое — `PixelCard`), логика — в
`MainActivitySimple.java` или в маленьком отдельном классе, как `DailyGiftCard`. Новую страницу
сделайте как `page_settings.xml`: ещё один `<include>` поверх главной, показывать через
`setVisibility`. Все тексты — в оба `strings.xml`.

## Фоны виджета и взаимодействия

Шесть фонов: три старые картинки (bg_type 1–3, сад по умолчанию) и три пиксельных осенних
(4 «Кленовый парк», 5 «Дождливый вечер», 6 «Тыквенное поле»). Фон хранится у каждого виджета
отдельно (`bg_type_<id>`). Пиксельные фоны рисует скрипт `tools/sprite_gen/scenery.py` (см. README
генератора). У аквариума свой фон — сцена, пиксельные фоны и взаимодействия его не касаются.

| Что | Где |
|---|---|
| Карточки фонов (на главном экране и на экране смены фона) | `res/layout/part_bg_cards.xml` + `BackgroundCards.java` |
| Смена фона у стоящего виджета (Android 12+: долгое нажатие → «Настройки») | `BackgroundPickerActivity.java`, `res/layout/activity_background_picker.xml` |
| Описание фонов: картинка, линия земли, предметы, дождь | `Backdrop.java` (данные — `GeneratedBackgrounds.java`) |
| Взаимодействия питомцев с фонами (таблица 7 × 3) | `BackgroundEvents.java`, `TABLE` |
| Как часто: не чаще раза в 25 с, не больше двух за пробуждение | `BackgroundEvents.MIN_GAP_MS`, `MAX_PER_SESSION`, первое — через `PetBehavior.FIRST_EVENT_AFTER_MS` |
| Сколько раз повторяется занятие (2–4) | `PetSpec.MIN_REPEATS`, `MAX_REPEATS`; свои у питомца — `.repeats(min, max)` |
| Веса занятий | `PetSpec`, строки `.does(...)`; вес взаимодействия — третий аргумент `row(...)` в таблице |

**Взаимодействие** — это одно занятие из трёх частей: питомец замечает предмет и идёт (или летит)
к нему, играет с ним, пока фон делает своё (падает лист, меняется предмет, капает дождь), и
заканчивает своей развязкой; потом жизнь идёт как обычно, другим занятием. Одна строка таблицы:

```java
row(PET_DOG, MAPLE_PARK, 30, "что происходит, словами")
    .goesTo("heap", ANIMATION_LICK)       // к какому предмету идёт и как на него смотрит (без goesTo — на месте)
    .act(ANIMATION_EAT)                   // что делает у предмета
    .state("burst", ONCE)                 // что делает фон: .state(...), .leaf(x, y) или .drops(x, y)
    .then(ANIMATION_LICK),                // развязка
```

- **Добавить питомца**: добавить ему три строки, по одной на фон (без строк у него просто нет
  событий). Брать можно его готовые анимации; пиксельному питомцу можно нарисовать свои позы
  (`ANIMATION_POSE_1`..`4`: анимации в `tools/sprite_gen/design/<pet>_template.py`, подключаются
  в `PetSpec` через `.poses("имя", ...)`).
- **Добавить фон**: модуль `tools/sprite_gen/bg_<имя>.py`, имя в `scenery.BACKGROUNDS`, номер и
  описание в `Backdrop.java`, карточка в `part_bg_cards.xml` и `BackgroundCards.TYPES`, по строке
  на каждого питомца в таблице, названия в оба `strings.xml`.
- Проверка таблицы — `BackgroundEventsTest`: у каждой пары есть строка, все анимации и предметы
  существуют, события редкие, детерминированные и не повторяют занятие два раза подряд.
- На эмуляторе (только debug-сборка) любое взаимодействие запускается сразу:
  `adb shell am broadcast -n com.aixdev.pixelpet/com.example.pixelpetwidget.DebugEventReceiver --ei widget ID`.

## Звуки

`SoundManager.get(context)` с методами `click()`, `toggle()`, `success()`, `error()` и `pet(petType)`.
Щенок и котёнок тоже отвечают двумя звуками по очереди: щенок — два «тяв», котёнок — «мяу» и мурлыканье
(синтез — `tools/sound_gen/puppy_kitten_sounds.py`).
У аквариума свои звуки: `pet(petType)` по очереди играет два «блюпа», `chosen(petType)` — бульканье
при выборе, `success(petType)` — поднимающуюся каплю вместо обычного успеха.
Кнопки и карточки щёлкают сами, переключатель — сам. Звуки не играют, если в настройках звук
выключен, громкость 0 или телефон на беззвучном. Один и тот же звук не повторяется чаще раза в 100 мс.

- **Заменить звук:** положите новый WAV с тем же именем в `res/raw/` (короткий, до 30 КБ).
- **Добавить звук:** положите `res/raw/sfx_имя.wav`, добавьте константу и её в массив `ALL`
  в `SoundManager.java` и короткий метод, который её играет.

В виджете звуков нет — это проверяет тест `SoundAndUiCatalogTest`.

## Debug-экран

Только в debug-сборке: на главном экране долго нажмите на логотип — откроется страница со всеми
компонентами в светлой и тёмной теме рядом. Или из консоли:

```
adb shell am start -n com.aixdev.pixelpet/com.example.pixelpetwidget.UiGalleryActivity
```

## Проверка после правок

```
./gradlew assembleDebug lintDebug testDebugUnitTest
```
