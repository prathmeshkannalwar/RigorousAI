package com.example.Rigorous_X

// Android Core Imports

// Coroutines and Gemini API Imports

// Markwon Imports
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.bold
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.asTextOrNull
import com.google.ai.client.generativeai.type.content
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appPreferences: AppPreferences

    // --- PROPERTIES FOR MULTI-TURN CHAT ---
    private lateinit var generativeModel: GenerativeModel
    private lateinit var chatHistory: MutableList<Content>

    // --- UI ELEMENTS ---
    private lateinit var tVResult: TextView
    private lateinit var eTPrompt: EditText
    private lateinit var scrollView: ScrollView
    private var generatingAnimator: ObjectAnimator? = null

    // --- ACTIVITY AND PERMISSION HANDLING ---
    private val settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        recreateIfThemeChanged()
    }
    private val OVERLAY_PERMISSION_REQ_CODE = 1234

    private val menuActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_NEW_CHAT -> clearChat()
                ACTION_COPY_RESPONSE -> copyLastResponse()
                ACTION_SHARE_RESPONSE -> shareLastResponse()
                ACTION_OPEN_SETTINGS -> {
                    val settingsIntent = Intent(this@MainActivity, SettingsActivity::class.java)
                    settingsLauncher.launch(settingsIntent)
                }
            }
        }
    }

    companion object {
        const val ACTION_NEW_CHAT = "com.example.Rigorous_X.NEW_CHAT"
        const val ACTION_COPY_RESPONSE = "com.example.Rigorous_X.COPY_RESPONSE"
        const val ACTION_SHARE_RESPONSE = "com.example.Rigorous_X.SHARE_RESPONSE"
        const val ACTION_OPEN_SETTINGS = "com.example.Rigorous_X.OPEN_SETTINGS"
    }

    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        appPreferences = AppPreferences(this)
        AppCompatDelegate.setDefaultNightMode(appPreferences.theme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        eTPrompt = findViewById(R.id.eTPrompt)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        tVResult = findViewById(R.id.tVResult)
        scrollView = findViewById(R.id.scrollView)

        setupGradientAndAnimator()
        initializeChat()

        btnSubmit.setOnClickListener {
            val prompt = eTPrompt.text.toString().trim()
            if (prompt.isNotEmpty()) {
                eTPrompt.text.clear()

                if (chatHistory.isEmpty()) tVResult.text = ""
                val userMessage = SpannableStringBuilder().bold { append("You: $prompt") }
                tVResult.append(userMessage)

                val generatingMessage = SpannableStringBuilder().append("\n\n").bold { append("Rigorous AI: ") }.append("Generating...")
                tVResult.append(generatingMessage)
                setGeneratingState(true)
                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }

                lifecycleScope.launch {
                    try {
                        val fullResponseText = StringBuilder()
                        generativeModel.startChat(history = chatHistory).sendMessageStream(prompt).collect { chunk ->
                            fullResponseText.append(chunk.text)
                        }

                        chatHistory.add(content("user") { text(prompt) })
                        chatHistory.add(content("model") { text(fullResponseText.toString()) })

                        setGeneratingState(false)

                        val formattedResponse: Spanned = markwon.toMarkdown(fullResponseText.toString())

                        val currentText = SpannableStringBuilder(tVResult.text)
                        val generatingText = "Generating..."
                        val startIndex = currentText.lastIndexOf(generatingText)

                        if (startIndex != -1) {
                            currentText.replace(startIndex, startIndex + generatingText.length, formattedResponse.subSequence(0, 1))
                            tVResult.text = currentText

                            val typewriterDelay = appPreferences.typewriterSpeed
                            for (i in 2..formattedResponse.length) {
                                tVResult.append(formattedResponse.subSequence(i - 1, i))
                                // --- SCROLLVIEW FIX: Auto-scroll is REMOVED from the loop ---
                                delay(typewriterDelay)
                            }
                        }

                    } catch (e: Exception) {
                        setGeneratingState(false)
                        appendMessageToUi("Error: ${e.message}", isUser = false, isError = true)
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a prompt.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- UI and State Management Functions ---

    private fun appendMessageToUi(message: String, isUser: Boolean, isError: Boolean = false) {
        if (chatHistory.isEmpty() && !isError) tVResult.text = ""

        val formattedMessage = SpannableStringBuilder()
        if (tVResult.text.isNotEmpty()) {
            formattedMessage.append("\n\n")
        }

        if (isUser) {
            formattedMessage.bold { append(message) }
        } else if (isError) {
            val currentText = tVResult.text.toString()
            if (currentText.endsWith("Generating...")) {
                val lastIndex = currentText.lastIndexOf("Rigorous AI: Generating...")
                if (lastIndex != -1) tVResult.text = currentText.substring(0, lastIndex)
            }
            formattedMessage.bold { append("Rigorous AI: ") }
            formattedMessage.append(message)
        }

        tVResult.append(formattedMessage)
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun setGeneratingState(isGenerating: Boolean) {
        if (isGenerating) {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.placeholderTextColor, typedValue, true)
            generatingAnimator?.start()
        } else {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.primaryTextColor, typedValue, true)
            tVResult.setTextColor(typedValue.data)
            generatingAnimator?.cancel()
            tVResult.alpha = 1.0f
        }
    }

    private fun recreateIfThemeChanged() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val savedNightMode = AppCompatDelegate.getDefaultNightMode()
        val currentTheme = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (savedNightMode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && currentTheme != savedNightMode) {
            recreate()
        }
    }

    // --- Lifecycle and Setup Functions ---

    override fun onStart() {
        super.onStart()
        checkOverlayPermissionAndStartService()
    }

    override fun onStop() {
        super.onStop()
        stopService(Intent(this, FloatingMenuService::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_NEW_CHAT)
            addAction(ACTION_COPY_RESPONSE)
            addAction(ACTION_SHARE_RESPONSE)
            addAction(ACTION_OPEN_SETTINGS)
        }
        registerReceiver(menuActionReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        recreateIfThemeChanged()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(menuActionReceiver)
    }

    private fun setupGradientAndAnimator() {
        val versionTextView = findViewById<TextView>(R.id.tv_version_watermark)
        val paint = versionTextView.paint
        val width = paint.measureText(versionTextView.text.toString())
        val textShader: Shader = LinearGradient(0f, 0f, width, versionTextView.textSize, intArrayOf(Color.parseColor("#4285F4"), Color.parseColor("#9B72CF"), Color.parseColor("#D96570")), null, Shader.TileMode.CLAMP)
        versionTextView.paint.shader = textShader

        generatingAnimator = ObjectAnimator.ofFloat(tVResult, "alpha", 1f, 0.2f).apply {
            duration = 800
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }
    }

    private fun initializeChat() {
        generativeModel = GenerativeModel(modelName = "gemini-1.5-flash-latest", apiKey = BuildConfig.GEMINI_API_KEY)
        chatHistory = mutableListOf()
        markwon = Markwon.builder(this).usePlugin(TablePlugin.create(this)).usePlugin(LinkifyPlugin.create()).usePlugin(object : AbstractMarkwonPlugin() { override fun configureVisitor(builder: MarkwonVisitor.Builder) { /* Markwon plugins */ } }).build()
        clearChat()
    }

    private fun clearChat() {
        chatHistory.clear()
        tVResult.text = "How can I Help You?"
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.primaryTextColor, typedValue, true)
        tVResult.setTextColor(typedValue.data)
        Toast.makeText(this, "New chat started", Toast.LENGTH_SHORT).show()
    }

    // --- Other Helper Functions ---

    private fun copyLastResponse() {
        val fullText = chatHistory.joinToString(separator = "\n\n") { content ->
            when (content.role) {
                "user" -> "You: ${content.parts.first().asTextOrNull() ?: ""}"
                "model" -> "Rigorous AI:\n${content.parts.first().asTextOrNull() ?: ""}"
                else -> ""
            }
        }
        if (fullText.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Chat History", fullText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Full conversation copied", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareLastResponse() {
        val fullText = chatHistory.joinToString(separator = "\n\n") { content ->
            when (content.role) {
                "user" -> "You: ${content.parts.first().asTextOrNull() ?: ""}"
                "model" -> "Rigorous AI:\n${content.parts.first().asTextOrNull() ?: ""}"
                else -> ""
            }
        }
        if (fullText.isNotEmpty()) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, fullText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Conversation via")
            startActivity(shareIntent)
        } else {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOverlayPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            startFloatingMenuService()
        }
    }

    private fun startFloatingMenuService() {
        startService(Intent(this, FloatingMenuService::class.java))
    }
}