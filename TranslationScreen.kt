package app.lenstranslatorpro

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TranslationScreen(vm: TranslationViewModel = viewModel()) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsState()
    val clipboard = LocalClipboardManager.current
    val speaker = remember { Speaker(ctx) }
    DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    var showSettings by remember { mutableStateOf(false) }

    fun toast(m: String) = Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show()
    val srcLang = if (s.source == "auto") vm.detected.ifBlank { "en" } else s.source

    fun doTranslate() {
        vm.translate { r -> if (s.autoSpeak) speaker.speak(r, s.target, s.speechRate, s.pitch) }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val t = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!t.isNullOrBlank()) {
            vm.input = t
            doTranslate()
        }
    }

    fun startVoice() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        if (s.source != "auto") i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, s.source)
        try { voiceLauncher.launch(i) } catch (e: Exception) { toast("التعرف على الصوت غير متاح على هذا الجهاز") }
    }

    fun startBubble() {
        if (!Settings.canDrawOverlays(ctx)) {
            toast("فعّل إذن العرض فوق التطبيقات ثم ارجع واضغط من جديد")
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
        } else {
            ContextCompat.startForegroundService(ctx, Intent(ctx, FloatingTranslateService::class.java))
        }
    }

    fun remind(min: Int) {
        ReminderHelper.schedule(ctx, min, vm.output.ifBlank { vm.input }.ifBlank { "تذكير" })
        toast("سيصلك تنبيه بعد $min دقيقة")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ===== الترويسة =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF5B4BDB), Color(0xFF0D9488))))
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🌐 LensTranslatorPro", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("ترجم نصًا أو صوتًا بسهولة", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ===== اختيار اللغات =====
            AppCard {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguagePicker(s.source, true, Modifier.weight(1f)) { c -> vm.update { it.copy(source = c) } }
                    FilledIconButton(
                        onClick = { if (s.source != "auto") vm.update { it.copy(source = it.target, target = it.source) } },
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) { Text("⇄", fontSize = 18.sp) }
                    LanguagePicker(s.target, false, Modifier.weight(1f)) { c -> vm.update { it.copy(target = c) } }
                }
            }

            // ===== الإدخال =====
            AppCard {
                Column {
                    TextField(
                        value = vm.input,
                        onValueChange = { vm.input = it },
                        placeholder = { Text("اكتب أو الصق النص هنا...") },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            TextButton(onClick = { startVoice() }) { Text("🎤 تحدث") }
                            TextButton(onClick = {
                                val t = clipboard.getText()?.text.orEmpty()
                                if (t.isNotBlank()) {
                                    vm.input = t
                                    if (s.autoTranslateOnPaste) doTranslate()
                                }
                            }) { Text("📋 لصق") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${vm.input.length}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            TextButton(onClick = { vm.input = ""; vm.output = ""; vm.extra = ""; vm.error = "" }) { Text("🗑 مسح") }
                        }
                    }
                }
            }

            // ===== زر الترجمة =====
            Button(
                onClick = { doTranslate() },
                enabled = !vm.loading && vm.input.isNotBlank(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (vm.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                } else {
                    Text("ترجمة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (vm.error.isNotBlank()) {
                Text(vm.error, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            // ===== النتيجة =====
            if (vm.output.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("الترجمة", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), fontSize = 13.sp)
                        Text(
                            vm.output,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 21.sp,
                            lineHeight = 31.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallAction("📄 نسخ الأصلي", Modifier.weight(1f)) { clipboard.setText(AnnotatedString(vm.input)); toast("تم نسخ الأصلي") }
                            SmallAction("📄 نسخ الترجمة", Modifier.weight(1f)) { clipboard.setText(AnnotatedString(vm.output)); toast("تم نسخ الترجمة") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallAction("🔊 الأصلي", Modifier.weight(1f)) { speaker.speak(vm.input, srcLang, s.speechRate, s.pitch) }
                            SmallAction("🔊 الترجمة", Modifier.weight(1f)) { speaker.speak(vm.output, s.target, s.speechRate, s.pitch) }
                        }
                    }
                }
            }

            // ===== أدوات ذكية =====
            SectionCard("✨ أدوات ذكية (محلية)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("شرح", Modifier.weight(1f), enabled = vm.output.isNotBlank()) { vm.explain() }
                    SmallAction("تلخيص", Modifier.weight(1f), enabled = vm.input.isNotBlank()) { vm.summarize() }
                }
                if (vm.extra.isNotBlank()) {
                    Text(vm.extra, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, lineHeight = 24.sp)
                }
            }

            // ===== تذكير =====
            SectionCard("⏰ تذكير") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 30, 60).forEach { m ->
                        SmallAction("$m دقيقة", Modifier.weight(1f)) { remind(m) }
                    }
                }
            }

            // ===== الفقاعة =====
            SectionCard("🫧 الفقاعة العائمة") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { startBubble() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("تشغيل") }
                    SmallAction("إيقاف", Modifier.weight(1f)) { ctx.stopService(Intent(ctx, FloatingTranslateService::class.java)) }
                }
                SliderRow("الشفافية", s.overlayOpacity, 0.3f, 1f) { v -> vm.update { it.copy(overlayOpacity = v) } }
                SliderRow("الحجم", s.overlaySize.toFloat(), 40f, 96f) { v -> vm.update { it.copy(overlaySize = v.toInt()) } }
                Text("التغييرات تُطبّق عند إعادة تشغيل الفقاعة", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            // ===== الإعدادات =====
            SectionCard("⚙️ الإعدادات") {
                SwitchRow("الوضع الداكن", s.darkMode) { v -> vm.update { it.copy(darkMode = v) } }
                TextButton(onClick = { showSettings = !showSettings }) {
                    Text(if (showSettings) "إخفاء المزيد ▲" else "المزيد ▼")
                }
                if (showSettings) {
                    SwitchRow("ترجمة تلقائية عند اللصق", s.autoTranslateOnPaste) { v -> vm.update { it.copy(autoTranslateOnPaste = v) } }
                    SwitchRow("نطق الترجمة تلقائيًا", s.autoSpeak) { v -> vm.update { it.copy(autoSpeak = v) } }
                    SliderRow("سرعة الصوت", s.speechRate, 0.5f, 2f) { v -> vm.update { it.copy(speechRate = v) } }
                    SliderRow("درجة الصوت", s.pitch, 0.5f, 2f) { v -> vm.update { it.copy(pitch = v) } }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AppCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SmallAction(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) { Text(label, fontSize = 13.sp, maxLines = 1) }
}

@Composable
private fun LanguagePicker(value: String, includeAuto: Boolean, modifier: Modifier, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = (if (includeAuto) listOf(Lang("auto", "تلقائي")) else emptyList()) + Languages.all
    Box(modifier) {
        FilledTonalButton(
            onClick = { open = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(Languages.name(value), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { l ->
                DropdownMenuItem(text = { Text(l.name) }, onClick = { onSelect(l.code); open = false })
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onFinished: (Float) -> Unit) {
    var local by remember(value) { mutableStateOf(value) }
    Column {
        Text("$label: ${"%.1f".format(local)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Slider(value = local, onValueChange = { local = it }, onValueChangeFinished = { onFinished(local) }, valueRange = min..max)
    }
}
