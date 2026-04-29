# OfficeDesk Конвертер

Android-приложение для локальной конвертации документов. Для публикации в RuStore.

**Форматы:** DOCX, DOC, RTF, ODT, TXT, MD, HTML, EPUB, PDF, XLSX, XLS.
Все файлы обрабатываются на устройстве, без сети.

---

## Быстрый старт: получить готовый APK через GitHub (без Android Studio, 5 минут)

1. Создай новый репозиторий на github.com (приватный или публичный — без разницы).
2. Скачай и распакуй этот ZIP. В папке проекта выполни:
   ```bash
   git init
   git add .
   git commit -m "init: officedesk converter"
   git branch -M main
   git remote add origin https://github.com/ТВОЙ_ЛОГИН/officedesk-converter.git
   git push -u origin main
   ```
3. Зайди на github.com → твой репозиторий → вкладка **Actions**.
4. Сборка `Build APK` запустится автоматически. Жди 4–7 минут.
5. Когда зелёная галочка — открой завершённый запуск и внизу скачай артефакты:
   - **officedesk-converter-debug** — APK для тестов на телефоне
   - **officedesk-converter-release** — APK для RuStore (нужно подписать своим ключом, см. ниже)
6. Внутри ZIP-артефакта будет `.apk`. Скинь на телефон и установи.

---

## Если CI упадёт на сборке

Это возможно — у Apache POI/iText бывают конфликты зависимостей на Android. Открой Actions → лог упавшей задачи. Самые частые проблемы и фиксы:

| Ошибка | Решение |
|---|---|
| `Duplicate class ...` | В `app/build.gradle` добавить `exclude group: '<имя>'` к проблемной зависимости |
| `Cannot fit requested classes` | `multiDexEnabled true` уже включен. Если всё равно — обнови `compileSdk` до 35 |
| `OutOfMemoryError` в Gradle | В `gradle.properties` увеличить `-Xmx2048m` до `-Xmx4096m` |
| `xmlbeans` крашится | Заменить `org.apache.xmlbeans:xmlbeans:5.2.0` на `5.0.3` |
| iText не найден | Открыть `app/build.gradle` и увеличить версию iText до 8.0.4 (требует `compileSdk 34+`) |

В крайнем случае открой проект в Android Studio — она лучше показывает реальные проблемы.

---

## Подпись APK для RuStore

RuStore требует подписанный APK. По умолчанию `release` подписан debug-ключом (только для тестов). Для публикации:

1. Создай ключ:
   ```bash
   keytool -genkey -v -keystore officedesk.keystore \
     -keyalg RSA -keysize 2048 -validity 10000 -alias officedesk
   ```
2. В `app/build.gradle` замени `signingConfig signingConfigs.debug` на:
   ```groovy
   android {
       signingConfigs {
           release {
               storeFile file('officedesk.keystore')
               storePassword 'твой_пароль'
               keyAlias 'officedesk'
               keyPassword 'твой_пароль'
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               minifyEnabled true
               shrinkResources true
               proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
           }
       }
   }
   ```
3. **Не коммить keystore в публичный репозиторий!** Лучше через GitHub Secrets.
4. Пересобери — в Actions появится подписанный release.

---

## Что положить в RuStore

В папке `marketing/`:
- `icon_512.png` — иконка приложения (512×512)
- `feature_graphic_1024x500.png` — баннер на странице приложения в RuStore (1024×500)
- `banner_vertical_1080x1920.png` — для рекламы / соцсетей (Stories формат)
- `banner_horizontal_1920x1080.png` — для рекламы / десктоп (Full HD)

В папке `legal/`:
- `PRIVACY.md` — политика конфиденциальности (опубликовать на сайте, ссылку добавить в `Screens.kt` и в карточку RuStore)
- `TERMS.md` — пользовательское соглашение

---

## Чеклист перед публикацией в RuStore

- [ ] Заменить `signingConfig` на свой ключ
- [ ] Опубликовать `PRIVACY.md` и `TERMS.md` по реальным URL (например на GitHub Pages бесплатно)
- [ ] В файле `Screens.kt` найти строки `officedesk.ru/privacy`, `officedesk.ru/terms`, `support@officedesk.ru` — заменить на свои
- [ ] В файле `Sharing.kt` ID Читалки (`ru.officedesk.reader`) — поменяешь, если у тебя другой
- [ ] Сделать 4–6 скриншотов реального интерфейса (не заставку!) — снимать на телефоне после установки
- [ ] В описании RuStore явно указать: «Конвертер работает только с локальными файлами пользователя»
- [ ] Возрастная категория **0+**

---

## Связь с Читалкой

Когда пользователь конвертирует документ в PDF/EPUB/TXT/HTML, появляется кнопка **«В читалке»**.

- Если установлена `ru.officedesk.reader` — открывается там.
- Если нет — открывается страница Читалки в RuStore.

В файле `Sharing.kt` строка `READER_PKG = "ru.officedesk.reader"` — поменяй, если ID Читалки будет другим.
