# LensTranslatorPro

تطبيق Android للترجمة (Kotlin + Jetpack Compose + Material 3).

## المميزات
- ترجمة نصية وصوتية (ML Kit يعمل على الجهاز، يحتاج إنترنت عند أول تحميل لكل لغة)
- اكتشاف اللغة تلقائيًا، 10 لغات
- نسخ الأصلي/المترجم، والاستماع لهما (TextToSpeech)
- فقاعة عائمة فوق التطبيقات (قابلة للسحب): نسخ/استماع لآخر ترجمة
- تذكير بعد 5/30/60 دقيقة
- إعدادات محفوظة (DataStore)
- أدوات "شرح" و"تلخيص" بسيطة تعمل محليًا (مكان مناسب لربط Gemini/OpenAI لاحقًا في AiTranslationManager.kt)

## البناء على GitHub (بدون تثبيت أي شيء)
1. ارفع كل الملفات إلى مستودع GitHub جديد.
2. افتح تبويب **Actions** ← **Build APK** ← انتظر حتى يصبح أخضر.
3. نزّل الملف من **Artifacts**: `LensTranslatorPro-debug-apk` ثم فك الضغط لتحصل على `app-debug.apk`.

## البناء في Android Studio
افتح المجلد، انتظر مزامنة Gradle، ثم Run أو Build > Build APK(s).
