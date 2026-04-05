# 📱 لوحة إسلامية — Islamic Dashboard

تطبيق Android إسلامي شامل مع 5 أقسام رئيسية.

---

## 🚀 طريقة بناء APK عبر GitHub (مجاني — بدون أي برامج)

### الخطوة 1: إنشاء حساب GitHub
إذا لم يكن لديك حساب: https://github.com/signup

### الخطوة 2: إنشاء Repository جديد
1. اذهب إلى https://github.com/new
2. اسم المشروع: `IslamicDashboard`
3. اجعله **Public**
4. اضغط **Create repository**

### الخطوة 3: رفع الملفات
```bash
# في terminal أو command prompt:
cd IslamicApp
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/IslamicDashboard.git
git push -u origin main
```

> **ملاحظة:** قبل رفع الملفات، تأكد من وجود `gradle-wrapper.jar`:
> ```bash
> gradle wrapper --gradle-version 8.2
> ```
> أو اذهب إلى Android Studio → Tools → Generate Gradle Wrapper

### الخطوة 4: انتظر البناء التلقائي
- اذهب إلى `https://github.com/YOUR_USERNAME/IslamicDashboard/actions`
- ستجد **workflow** يعمل تلقائياً
- بعد ~5 دقائق ✅ APK جاهز للتحميل!

### الخطوة 5: تحميل APK
- اذهب إلى **Actions** → اختر آخر run → **Artifacts**
- حمّل `Islamic-Dashboard-Debug.apk`
- أو اذهب إلى **Releases** للنسخة الكاملة

---

## 📲 تثبيت APK على الهاتف
1. انقل APK للهاتف
2. اذهب إلى **الإعدادات → الأمان → مصادر غير معروفة** ✅
3. افتح ملف APK وثبّته

---

## 🗂️ هيكل المشروع
```
IslamicApp/
├── .github/workflows/build.yml   ← GitHub Actions (البناء التلقائي)
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── home.html         🏠 الرئيسية
│   │   │   ├── prayer.html       🕌 مواقيت الصلاة
│   │   │   ├── cosmos.html       🌙 الشمس والقمر
│   │   │   ├── events.html       ✨ الظواهر الكونية
│   │   │   └── converter.html    📅 تحويل التاريخ
│   │   ├── java/.../MainActivity.kt
│   │   ├── AndroidManifest.xml
│   │   └── res/
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradlew
```

---

## ✨ مميزات التطبيق

| Tab | المحتوى |
|-----|---------|
| 🏠 الرئيسية | ساعة حية + تاريخ هجري/ميلادي + طقس + خريطة الدولة الصماء مع PIN |
| 🕌 الصلاة | مواقيت الصلاة + عداد تنازلي + اتجاه القبلة + بوصلة |
| 🌙 الشمس والقمر | قوس الشمس + أطوار القمر على قوس mirror + كسوف/خسوف |
| ✨ الظواهر | قمر عملاق/أزرق/دموي + أمطار الشهب + اقترانات كوكبية |
| 📅 التاريخ | تحويل هجري↔ميلادي + مرجع الأشهر الهجرية |

---

## 🛠️ بناء محلي (Android Studio)
```
1. افتح Android Studio
2. File → Open → اختر مجلد IslamicApp
3. انتظر Gradle Sync
4. Build → Generate Signed Bundle/APK
```

---

## 📋 المتطلبات
- Android 7.0+ (API 24)
- اتصال بالإنترنت لتحميل المواقيت والخرائط
