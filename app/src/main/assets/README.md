# Hacked Keyboard GEM

## Fork Update (2024-2026): ##

**Modernized:** Upgraded to **Target SDK 34** (Android 14), **Compile SDK 36**, **AGP 9.1.0**, and **Gradle 9.4.1**. All legacy resource errors and namespace issues have been resolved to meet modern Play Store requirements.

**Modern AMOLED Theme:** A premium black theme optimized for modern OLED displays. Keys feature a semi-transparent "frosted glass" effect, modern rounded corners, and sleek custom **Cyan LED indicators** for toggle keys (Ctrl, Alt, Shift).

**Modern Action Keys Option:** Added a new toggle in the **Settings > Key behavior settings** menu to dynamically swap the physical locations of the **Enter** and **Backspace** keys. This matches modern mobile muscle memory while preserving the classic PC layout logic.

**Reverse Tab Key:** New feature to reverse Tab key behavior in web entry modes, improving navigation in complex web forms.

**Suggestion Improvements:**
- Added a "Minimum number letters" slider (2-16) in settings to control exactly when suggestions appear.
- Fixed and improved word completion and prediction logic for a smoother typing experience.

**Enhanced Language & Dictionary Support:**
- **Search Bar:** The "Input Language" selection page now includes a search bar for quickly finding specific layouts.
- **Layout Labels:** Visual indicators show which layouts support 4-row or 5-row modes.
- **External Dictionaries:** The "Detected external dictionaries" UI now lists all installed dictionary packs.
- **AnySoftKeyboard Support:** Native support for [AnySoftKeyboard dictionary packs](https://github.com/AnySoftKeyboard/AnySoftKeyboard), significantly expanding available language support.
    - *Note:* To use the **English dictionary**, please install the main **AnySoftKeyboard** application. The "Hacked Keyboard GEM" will then automatically detect and use its built-in English dictionary resources.

**Improved Compatibility:** Full support for modern Android 13/14+ features, including gesture navigation compatibility and updated notification channels.

## Original Readme ##

**STATUS:** *This project has been modernized to target API level 34. The original warnings about it being "ancient" and incompatible with new APIs have been addressed. Language switching and popup keys are being maintained for compatibility with the latest Android systems.*

Are you missing the key layout you're used to from your computer when using an Android device? This software keyboard has separate number keys, punctuation in the usual places, and arrow keys. It is based on the AOSP Gingerbread soft keyboard, so it supports multitouch for the modifier keys.

This keyboard is especially useful if you use ConnectBot for SSH access. It provides working Tab/Ctrl/Esc keys, and the arrow keys are essential for devices such as the Xoom tablet or Nexus S that don't have a trackball or D-Pad.

The supported keyboard layouts include Armenian (Հայերեն), Arabic (العربية),
British (en\_GB), Bulgarian (български език), Czech (Čeština), Danish (dansk),
Carpalx English (language "en-CX"), Dvorak English (language "en-DV"), English
(QWERTY), Finnish (Suomi), French (Français, AZERTY), German (Deutsch, QWERTZ),
German Neo2 (Deutsch, language "de-NE"),
Greek (ελληνικά), Hebrew (עברית), Hungarian (Magyar), Italian (Italiano), Lao
(ພາສາລາວ), Norwegian (Norsk bokmål), Persian (فارسی), Portuguese (Português),
Romanian (Română), Russian (Русский), Russian phonetic (Русский, ru-rPH),
Serbian (Српски), Slovak (Slovenčina), Slovenian
(Slovenščina)/Bosnian/Croatian/Latin Serbian, Spanish (Español, Español
Latinoamérica), Swedish (Svenska), Tamil (தமிழ்), Thai (ไทย), Turkish (Türkçe),
and Ukrainian (українська мова).

To install, get **[Hacked Keyboard GEM](https://play.google.com/store/apps/details?id=org.n0pocketworkstation.pckeyboard)**
from the Play Store, plus optional [dictionary
packs](https://play.google.com/store/apps/developer?id=Klaus+Weidner).

## Additional resources ##

Forked from the original project by Klaus Weidner. This version is maintained at:
**[GitHub: oblus/hackerskeyboard](https://github.com/oblus/hackerskeyboard)**

See the **[Original Release Notes](https://github.com/klausw/hackerskeyboard/wiki/ReleaseNotes)** for historical changes.

Having problems? See the **[User's Guide](https://github.com/klausw/hackerskeyboard/wiki/UsersGuide)** and **[FAQ](https://github.com/klausw/hackerskeyboard/wiki/FrequentlyAskedQuestions)**.

Comments, requests, or contributions? Check the [issue tracker](https://github.com/oblus/hackerskeyboard/issues) on the new fork.

Application developers: see [the page about keyboard support in applications](https://github.com/klausw/hackerskeyboard/wiki/KeyboardSupportInApplications) if you want to enable the additional keys in your Android application.
