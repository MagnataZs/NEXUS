package com.nexusroot

import java.io.BufferedReader
import java.io.InputStreamReader

data class RootResult(val ok: Boolean, val output: String)

object RootAccess {
    fun run(command: String): RootResult = try {
        val p = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val out = BufferedReader(InputStreamReader(p.inputStream)).readText()
        RootResult(p.waitFor() == 0, out.trim())
    } catch (e: Exception) {
        RootResult(false, e.message ?: "Falha ROOT")
    }
}
