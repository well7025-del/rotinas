package br.com.rotinas.etiquetas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import br.com.rotinas.etiquetas.TextTools.upper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Desenha a etiqueta em uma folha A5, sempre no maior tamanho de letra que
 * couber. O mesmo código serve para a pré-visualização na tela e para o PDF
 * que vai para a impressora — só muda o tamanho do canvas.
 *
 * Tudo é medido em fração da altura/largura da página, então o resultado é
 * idêntico em qualquer resolução.
 */
object LabelRenderer {

    /** A5 em pontos PostScript (148 x 210 mm), usado pelo PDF. */
    const val A5_WIDTH_POINTS = 420
    const val A5_HEIGHT_POINTS = 595

    private const val MARGIN_RATIO = 0.055f
    private const val MIN_TEXT_SIZE = 4f

    /** Do topo de uma linha em caixa alta até a linha de base. */
    private const val ASCENT = 0.82f

    /** Sobra abaixo da linha de base, para o rabo do "g" e do "ç". */
    private const val DESCENDER = 0.24f

    private val condensedBold: Typeface =
        Typeface.create("sans-serif-condensed", Typeface.BOLD)
    private val condensed: Typeface =
        Typeface.create("sans-serif-condensed", Typeface.NORMAL)

    fun draw(canvas: Canvas, width: Float, height: Float, order: Order, date: Date = Date()) {
        canvas.drawColor(Color.WHITE)

        val margin = width * MARGIN_RATIO
        val left = margin
        val right = width - margin
        val contentWidth = right - left

        drawBorder(canvas, width, height, margin)

        val rule = Paint().apply {
            color = Color.BLACK
            strokeWidth = height * 0.0035f
            isAntiAlias = true
        }

        var y = margin * 1.25f

        // ---- faixa superior: "PEDIDO" e a data ----
        val stripSize = height * 0.022f
        val stripPaint = textPaint(condensed, stripSize)
        y += stripSize
        canvas.drawText("PEDIDO", left, y, stripPaint)
        val dateText = DATE_FORMAT.format(date)
        canvas.drawText(dateText, right - stripPaint.measureText(dateText), y, stripPaint)
        y += height * 0.012f
        canvas.drawLine(left, y, right, y, rule)

        val hasItems = order.items.isNotEmpty()
        val hasPlace = order.place.isNotBlank()

        // ---- mede o bloco do cliente antes de desenhar ----
        // Sem itens, esse bloco é a etiqueta inteira e vale centralizá-lo.
        val customer = upper(order.customer.ifBlank { "SEM NOME" })
        val namePaint = textPaint(condensedBold, 10f)
        val nameLimit = if (hasItems) height * 0.24f else height * 0.40f
        val nameSize = fitTextSize(namePaint, customer, contentWidth, nameLimit, maxLines = 2)
        namePaint.textSize = nameSize
        val nameLines = wrap(customer, namePaint, contentWidth)

        val placePaint = textPaint(condensedBold, 10f)
        var placeSize = 0f
        if (hasPlace) {
            val placeLimit = if (hasItems) height * 0.11f else height * 0.16f
            placeSize = minOf(
                fitTextSize(placePaint, upper(order.place), contentWidth, placeLimit, maxLines = 1),
                nameSize * 0.78f,
            )
            placePaint.textSize = placeSize
        }

        val placeStep = if (hasPlace) nameSize * DESCENDER + placeSize * ASCENT else 0f
        val blockHeight = nameSize * ASCENT +
            (nameLines.size - 1) * lineHeight(namePaint) +
            placeStep +
            (if (hasPlace) placeSize else nameSize) * DESCENDER

        y += height * 0.03f
        if (!hasItems) {
            // Centraliza verticalmente entre a régua do cabeçalho e o pé da folha.
            y += ((height - margin - y) - blockHeight).coerceAtLeast(0f) / 2f
        }

        // ---- nome do cliente: o maior texto da etiqueta ----
        y += nameSize * ASCENT
        nameLines.forEachIndexed { index, line ->
            canvas.drawText(line, left, y + index * lineHeight(namePaint), namePaint)
        }
        y += (nameLines.size - 1) * lineHeight(namePaint)

        // ---- localidade ----
        if (hasPlace) {
            y += placeStep
            canvas.drawText(upper(order.place), left, y, placePaint)
        }

        // Espaço reservado no pé da folha, descontado antes de desenhar os itens
        // para que nada se sobreponha.
        val hasNote = order.note.isNotBlank()
        val footerBand = if (hasItems) height * 0.075f else 0f
        val noteBand = if (hasNote) height * 0.085f else 0f
        val bottomLimit = height - margin - footerBand - noteBand

        if (hasItems) {
            y += (if (hasPlace) placeSize else nameSize) * DESCENDER + height * 0.015f
            canvas.drawLine(left, y, right, y, rule)
            y += height * 0.03f
            drawItems(canvas, order.items, left, y, contentWidth, bottomLimit - y, nameSize)
        }

        if (hasNote) {
            val notePaint = textPaint(condensed, height * 0.028f)
            var noteY = height - margin - footerBand - noteBand + notePaint.textSize
            wrap("Obs.: ${order.note}", notePaint, contentWidth).take(2).forEach { line ->
                canvas.drawText(line, left, noteY, notePaint)
                noteY += lineHeight(notePaint)
            }
        }

        // ---- rodapé: total de peças à esquerda, quantidade de itens à direita ----
        if (hasItems) {
            val footerPaint = textPaint(condensed, height * 0.026f)
            val baseline = height - margin
            canvas.drawLine(left, baseline - footerBand * 0.55f, right, baseline - footerBand * 0.55f, rule)

            val pieces = if (order.totalPieces == 1) "1 peça" else "${order.totalPieces} peças"
            canvas.drawText("TOTAL: $pieces", left, baseline, footerPaint)

            val itemCount = "${order.items.size} " + if (order.items.size == 1) "item" else "itens"
            canvas.drawText(itemCount, right - footerPaint.measureText(itemCount), baseline, footerPaint)
        }
    }

    /**
     * Bloco de itens: quantidade à esquerda, descrição no meio e tamanho à
     * direita. A letra cresce até ocupar todo o espaço que sobrou na folha.
     */
    private fun drawItems(
        canvas: Canvas,
        items: List<OrderItem>,
        left: Float,
        top: Float,
        width: Float,
        availableHeight: Float,
        nameSize: Float,
    ) {
        if (availableHeight <= 0f) return

        val paint = textPaint(condensedBold, 10f)
        val maxSize = minOf(nameSize * 0.85f, availableHeight)
        val size = fitItemsSize(paint, items, width, availableHeight, maxSize)
        paint.textSize = size

        val metrics = itemMetrics(paint, items, width)
        val line = lineHeight(paint)
        val gap = size * 0.35f

        var y = top + size
        items.forEach { item ->
            val quantity = "${item.quantity}"
            canvas.drawText(quantity, left, y, paint)

            val descLeft = left + metrics.quantityColumn
            val descWidth = width - metrics.quantityColumn - metrics.sizeColumn
            val lines = wrap(item.description, paint, descWidth)

            lines.forEachIndexed { index, text ->
                canvas.drawText(text, descLeft, y + index * line, paint)
            }

            if (item.size.isNotBlank()) {
                canvas.drawText(item.size, left + width - paint.measureText(item.size), y, paint)
            }

            y += lines.size * line + gap
        }
    }

    private data class ItemMetrics(val quantityColumn: Float, val sizeColumn: Float)

    private fun itemMetrics(paint: Paint, items: List<OrderItem>, width: Float): ItemMetrics {
        val widestQuantity = items.maxOf { paint.measureText("${it.quantity}") }
        val widestSize = items.filter { it.size.isNotBlank() }
            .maxOfOrNull { paint.measureText(it.size) } ?: 0f
        val space = paint.measureText("  ")
        val quantityColumn = widestQuantity + space
        val sizeColumn = if (widestSize > 0f) widestSize + space else 0f
        // Nunca deixa as colunas laterais engolirem a descrição.
        val maxSide = width * 0.45f
        return ItemMetrics(
            quantityColumn = minOf(quantityColumn, maxSide),
            sizeColumn = minOf(sizeColumn, maxSide),
        )
    }

    /** Maior tamanho de letra em que todos os itens ainda cabem na altura disponível. */
    private fun fitItemsSize(
        paint: Paint,
        items: List<OrderItem>,
        width: Float,
        maxHeight: Float,
        maxSize: Float,
    ): Float {
        var low = MIN_TEXT_SIZE
        var high = maxOf(MIN_TEXT_SIZE, maxSize)
        var best = MIN_TEXT_SIZE

        repeat(24) {
            val candidate = (low + high) / 2f
            paint.textSize = candidate
            if (itemsHeight(paint, items, width, candidate) <= maxHeight) {
                best = candidate
                low = candidate
            } else {
                high = candidate
            }
        }
        return best
    }

    private fun itemsHeight(paint: Paint, items: List<OrderItem>, width: Float, size: Float): Float {
        val metrics = itemMetrics(paint, items, width)
        val descWidth = width - metrics.quantityColumn - metrics.sizeColumn
        if (descWidth <= size * 0.5f) return Float.MAX_VALUE

        val line = lineHeight(paint)
        val gap = size * 0.35f
        var total = 0f
        items.forEach { item ->
            // Partir uma palavra no meio ("Camis / a Sport") é ilegível: rejeita
            // o tamanho em que a maior palavra não cabe inteira na coluna.
            if (!wordsFit(item.description, paint, descWidth)) return Float.MAX_VALUE
            // Mais de duas linhas por item vira parede de texto: rejeita também.
            val lines = wrap(item.description, paint, descWidth)
            if (lines.size > 2) return Float.MAX_VALUE
            total += lines.size * line + gap
        }
        return total - gap
    }

    // ---------------------------------------------------------- desenho base

    private fun drawBorder(canvas: Canvas, width: Float, height: Float, margin: Float) {
        val border = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = height * 0.004f
            isAntiAlias = true
        }
        val inset = margin * 0.45f
        canvas.drawRect(inset, inset, width - inset, height - inset, border)
    }

    private fun textPaint(typeface: Typeface, size: Float) = Paint().apply {
        this.typeface = typeface
        textSize = size
        color = Color.BLACK
        isAntiAlias = true
        isSubpixelText = true
    }

    private fun lineHeight(paint: Paint): Float = paint.textSize * 1.12f

    /** Toda palavra cabe inteira na largura dada? */
    private fun wordsFit(text: String, paint: Paint, maxWidth: Float): Boolean =
        TextTools.squeeze(text).split(" ").all { paint.measureText(it) <= maxWidth }

    /**
     * Maior tamanho de letra em que [text] cabe em [maxWidth] x [maxHeight]
     * usando no máximo [maxLines] linhas.
     */
    private fun fitTextSize(
        paint: Paint,
        text: String,
        maxWidth: Float,
        maxHeight: Float,
        maxLines: Int,
    ): Float {
        if (text.isBlank() || maxWidth <= 0f || maxHeight <= 0f) return MIN_TEXT_SIZE

        var low = MIN_TEXT_SIZE
        var high = maxHeight
        var best = MIN_TEXT_SIZE

        repeat(24) {
            val candidate = (low + high) / 2f
            paint.textSize = candidate
            val lines = wrap(text, paint, maxWidth)
            val height = lines.size * lineHeight(paint)
            val fits = lines.size <= maxLines &&
                height <= maxHeight &&
                wordsFit(text, paint, maxWidth) &&
                lines.all { paint.measureText(it) <= maxWidth }
            if (fits) {
                best = candidate
                low = candidate
            } else {
                high = candidate
            }
        }
        return best
    }

    /**
     * Quebra por palavras. Palavra que sozinha não cabe é cortada no meio,
     * para nunca estourar a margem.
     */
    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val clean = TextTools.squeeze(text)
        if (clean.isEmpty()) return listOf("")
        if (maxWidth <= 0f) return listOf(clean)
        if (paint.measureText(clean) <= maxWidth) return listOf(clean)

        val lines = mutableListOf<String>()
        var current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder()
            }
        }

        clean.split(" ").forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                flush()
                if (paint.measureText(word) <= maxWidth) {
                    current = StringBuilder(word)
                } else {
                    lines += breakLongWord(word, paint, maxWidth, into = { current = StringBuilder(it) })
                }
            }
        }
        flush()
        return if (lines.isEmpty()) listOf(clean) else lines
    }

    /** Corta uma palavra gigante em pedaços; devolve os pedaços cheios e devolve o resto via [into]. */
    private fun breakLongWord(
        word: String,
        paint: Paint,
        maxWidth: Float,
        into: (String) -> Unit,
    ): List<String> {
        val chunks = mutableListOf<String>()
        var chunk = StringBuilder()
        word.forEach { char ->
            if (paint.measureText(chunk.toString() + char) <= maxWidth || chunk.isEmpty()) {
                chunk.append(char)
            } else {
                chunks += chunk.toString()
                chunk = StringBuilder().append(char)
            }
        }
        into(chunk.toString())
        return chunks
    }

    /** Pré-visualização na tela, no formato exato do A5. */
    fun preview(widthPx: Int, order: Order): Bitmap {
        val width = widthPx.coerceAtLeast(120)
        val height = (width * A5_HEIGHT_POINTS / A5_WIDTH_POINTS.toFloat()).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), width.toFloat(), height.toFloat(), order)
        return bitmap
    }

    private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
}
