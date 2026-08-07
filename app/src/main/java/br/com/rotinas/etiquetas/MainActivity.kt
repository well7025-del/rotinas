package br.com.rotinas.etiquetas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import br.com.rotinas.etiquetas.databinding.ActivityMainBinding

/**
 * Tela única: uma prévia da etiqueta e dois botões — GRAVAR e IMPRIMIR.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var voice: VoiceInput
    private lateinit var places: PlaceCatalog
    private lateinit var parser: OrderParser

    private var order: Order = Order(customer = "")

    private val requestMicrophone =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening() else showStatus(getString(R.string.erro_permissao))
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        places = PlaceCatalog(this)
        parser = OrderParser(places.all())
        voice = VoiceInput(this)

        binding.botaoGravar.setOnClickListener { onRecordPressed() }
        binding.botaoImprimir.setOnClickListener { imprimirEtiqueta() }
        binding.botaoEditar.setOnClickListener { showEditDialog() }
        binding.botaoLimpar.setOnClickListener { clear() }
        binding.botaoCidades.setOnClickListener { showPlacesDialog() }

        savedInstanceState?.getString(STATE_TEXT)?.let { order = parser.parse(it) }

        binding.previaEtiqueta.doOnLayout { renderPreview() }
        updateButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!order.isEmpty) outState.putString(STATE_TEXT, order.toPlainText())
    }

    override fun onStop() {
        super.onStop()
        voice.stop()
        updateButtons()
    }

    // ------------------------------------------------------------- gravação

    private fun onRecordPressed() {
        if (voice.isListening) {
            voice.stopListening()
            showStatus(getString(R.string.dica_pronto))
            return
        }

        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

        if (granted) startListening() else requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        voice.start(object : VoiceInput.Listener {
            override fun onReady() {
                showStatus(getString(R.string.dica_fale_agora))
                updateButtons()
            }

            override fun onPartial(text: String) = showStatus(text)

            override fun onResult(text: String) {
                order = parser.parse(text)
                renderPreview()
                showStatus(getString(R.string.dica_pronto))
                updateButtons()
            }

            override fun onError(message: String) {
                showStatus(message)
                updateButtons()
            }

            override fun onVolume(level: Float) {
                // Um leve "respirar" no botão mostra que o microfone está ouvindo.
                binding.botaoGravar.alpha = 0.75f + level * 0.25f
            }
        })
        updateButtons()
    }

    // ------------------------------------------------------------ impressão

    private fun imprimirEtiqueta() {
        if (order.isEmpty) {
            toast(getString(R.string.nada_para_imprimir))
            return
        }

        val manager = getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (manager == null) {
            toast(getString(R.string.erro_sem_impressao))
            return
        }

        // O tamanho A5 já vai escolhido; a impressora que o sistema encontrar
        // (USB, Wi-Fi ou Wi-Fi Direct) aparece na janela do Android.
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        val jobName = "${getString(R.string.trabalho_impressao)} — ${order.customer}"
        manager.print(jobName, LabelPrintAdapter(this, order), attributes)
    }

    // --------------------------------------------------------------- edição

    private fun showEditDialog() {
        val input = EditText(this).apply {
            setText(order.toPlainText())
            setSelection(text.length)
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            minLines = 3
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.titulo_editar)
            .setView(pad(input))
            .setPositiveButton(R.string.salvar) { _, _ ->
                order = parser.parse(input.text.toString())
                renderPreview()
                showStatus(getString(R.string.dica_pronto))
                updateButtons()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun showPlacesDialog() {
        val input = EditText(this).apply {
            setText(places.custom().joinToString("\n"))
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_WORDS
            minLines = 5
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.titulo_cidades)
            .setMessage(R.string.ajuda_cidades)
            .setView(pad(input))
            .setPositiveButton(R.string.salvar) { _, _ ->
                places.saveCustom(input.text.toString().split("\n"))
                parser = OrderParser(places.all())
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private fun clear() {
        voice.stop()
        order = Order(customer = "")
        renderPreview()
        showStatus(getString(R.string.dica_inicial))
        updateButtons()
    }

    // ------------------------------------------------------------------ tela

    private fun renderPreview() {
        val view = binding.previaEtiqueta
        val width = view.width
        if (width <= 0) {
            view.doOnLayout { renderPreview() }
            return
        }

        if (order.isEmpty) {
            view.setImageDrawable(null)
            return
        }

        // A altura do quadro manda quando ela é o lado mais apertado: assim a
        // folha aparece inteira, sem cortar.
        val fromHeight = (view.height * LabelRenderer.A5_WIDTH_POINTS /
            LabelRenderer.A5_HEIGHT_POINTS.toFloat()).toInt()
        val target = if (fromHeight in 1 until width) fromHeight else width
        view.setImageBitmap(LabelRenderer.preview(target.coerceAtMost(MAX_PREVIEW_PX), order))
    }

    private fun updateButtons() {
        val listening = voice.isListening
        binding.botaoGravar.alpha = 1f
        binding.botaoGravar.text =
            getString(if (listening) R.string.botao_ouvindo else R.string.botao_gravar)
        binding.botaoGravar.backgroundTintList =
            tint(if (listening) R.color.gravando else R.color.gravar)

        val canPrint = !order.isEmpty
        binding.botaoImprimir.isEnabled = canPrint
        binding.botaoImprimir.backgroundTintList =
            tint(if (canPrint) R.color.imprimir else R.color.imprimir_inativo)
    }

    private fun tint(colorRes: Int) =
        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))

    private fun showStatus(message: String) {
        binding.statusTexto.text = message
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    /** Dá respiro ao EditText dentro do AlertDialog. */
    private fun pad(view: EditText): LinearLayout = LinearLayout(this).apply {
        val space = (24 * resources.displayMetrics.density).toInt()
        setPadding(space, space / 2, space, 0)
        addView(
            view,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
    }

    private companion object {
        const val STATE_TEXT = "pedido"
        const val MAX_PREVIEW_PX = 1000
    }
}
