package com.nexusroot

import java.io.BufferedReader
import java.io.InputStreamReader

data class RootResult(val success: Boolean, val output: String)

object RootAccess {
    fun run(command: String): RootResult {
        return try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val exit = process.waitFor()
            RootResult(exit == 0, output.trim())
        } catch (e: Exception) {
            RootResult(false, e.message ?: "Erro ao executar ROOT")
        }
    }
}
