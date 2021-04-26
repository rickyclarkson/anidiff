import com.github.difflib.DiffUtils
import com.github.difflib.patch.AbstractDelta
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.awt.Graphics
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.WindowConstants

fun main(args: Array<String>) {
    val files: List<DiffFile> = args.map { filename ->
        DiffFile(BufferedReader(InputStreamReader(FileInputStream(filename))).use { it.readLines() }.toImmutableList())
    }

    val interpolated = LinkedHashSet<DiffFile>()
    for ((first, second) in files.zipWithNext()) {
        interpolated.addAll(interpolate(first, second))
    }

    val interpolatedList = interpolated.toList()

    val frame = JFrame()
    frame.contentPane = object : JPanel() {
        var frameNumber = 0

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val diffFile = interpolatedList[frameNumber % interpolatedList.size]
            diffFile.draw(g)
            frameNumber++
        }
    }
    Timer(200) { _ -> frame.contentPane.repaint() }.apply {
        isRepeats = true
        start()
    }
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.extendedState = JFrame.MAXIMIZED_BOTH
    frame.isVisible = true
}

data class DiffFile(val lines: ImmutableList<String>) {
    fun draw(g: Graphics) {
        g.font = g.font.deriveFont(24.0F)
        var y = 40
        for (line in lines) {
            g.drawString(line, 10, y)
            y += 40
        }
    }
}

/**
 * This version tries to make the differences smaller, so that animation is smoother.
 */
fun interpolate(from: DiffFile, to: DiffFile): List<DiffFile> {
    if (from == to) { return listOf(from) }
    val patch = DiffUtils.diff(from.lines, to.lines)
    val next = DiffFile(patch.deltas[0].applyDeltaAndReturn(from.lines).toImmutableList())
    val rest = interpolate(next, to).toMutableList()
    rest.addAll(0, interpolateDelta(patch.deltas[0], from))
    return rest
}

fun interpolateDelta(delta: AbstractDelta<String>, from: DiffFile): ImmutableList<DiffFile> {
    if (delta.source.position == delta.target.position) {
        return linewiseInterpolation(delta, from)
    }
    val results = mutableListOf(from)
    val lines = from.lines.toMutableList()
    for (line in delta.source.lines) {
        lines.removeAt(delta.source.position)
        results.add(DiffFile(lines.toImmutableList()))
    }
    var position = delta.target.position
    for (line in delta.target.lines) {
        lines.add(position, line)
        position++
        results.add(DiffFile(lines.toImmutableList()))
    }
    return results.toImmutableList()
}

fun linewiseInterpolation(delta: AbstractDelta<String>, from: DiffFile): ImmutableList<DiffFile> {
    val results = mutableListOf(from)
    val lines = from.lines.toMutableList()
    var index = delta.source.position
    for ((_, second) in delta.source.lines.zip(delta.target.lines)) {
        lines.removeAt(index)
        lines.add(index, second)
        index++
        results.add(DiffFile(lines.toImmutableList()))
    }
    if (delta.source.lines.size > delta.target.lines.size) {
        for (line in delta.source.lines.subList(delta.target.lines.size, delta.source.lines.size)) {
            lines.removeAt(index + 1)
            results.add(DiffFile(lines.toImmutableList()))
        }
    }
    if (delta.source.lines.size < delta.target.lines.size) {
        for (line in delta.target.lines.subList(delta.source.lines.size, delta.target.lines.size)) {
            lines.add(index, line)
            index++
            results.add(DiffFile(lines.toImmutableList()))
        }
    }
    return results.toImmutableList()
}

fun <T> AbstractDelta<T>.applyDeltaAndReturn(lines: List<T>): List<T> {
    val mutable = lines.toMutableList()
    applyTo(mutable)
    return mutable
}
