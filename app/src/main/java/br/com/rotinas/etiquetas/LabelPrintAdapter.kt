package br.com.rotinas.etiquetas

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream
import java.io.IOException

/**
 * Gera o PDF de uma página com a etiqueta e entrega para o serviço de
 * impressão do Android. Quem descobre a impressora (USB, Wi-Fi, Mopria,
 * Wi-Fi Direct, HP/Epson/Brother) é o próprio sistema — o aplicativo só
 * precisa entregar a página no tamanho certo.
 */
class LabelPrintAdapter(
    private val context: Context,
    private val order: Order,
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        pdfDocument?.close()
        pdfDocument = PrintedPdfDocument(context, newAttributes)

        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(fileName())
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        // "true" porque o desenho depende do tamanho de papel escolhido.
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val document = pdfDocument
        if (document == null) {
            callback.onWriteFailed(context.getString(R.string.erro_impressao))
            return
        }

        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }

        val page = document.startPage(0)
        try {
            // Desenha dentro da área imprimível informada pela impressora.
            val content = page.info.contentRect
            page.canvas.translate(content.left.toFloat(), content.top.toFloat())
            LabelRenderer.draw(
                canvas = page.canvas,
                width = content.width().toFloat(),
                height = content.height().toFloat(),
                order = order,
            )
            document.finishPage(page)

            FileOutputStream(destination.fileDescriptor).use { output ->
                document.writeTo(output)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: IOException) {
            callback.onWriteFailed(error.message)
        } finally {
            document.close()
            pdfDocument = null
        }
    }

    override fun onFinish() {
        pdfDocument?.close()
        pdfDocument = null
    }

    private fun fileName(): String {
        val name = order.customer.ifBlank { "etiqueta" }
            .replace(Regex("[^\\p{L}\\p{N} ]"), "")
            .replace(' ', '_')
            .take(40)
        return "Etiqueta_$name.pdf"
    }
}
