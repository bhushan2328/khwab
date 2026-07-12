package com.toblad.khwab.demo

import com.toblad.khwab.core.brain.Brain
import com.toblad.khwab.core.executor.MockCommandExecutor

object KhwabConsole {

    @JvmStatic
    fun main(args: Array<String>) {

        val brain = Brain()
        val executor = MockCommandExecutor()

        println("===================================")
        println("      KHWAB CORE CONSOLE")
        println("===================================")
        println("Type 'exit' to quit.")
        println()

        while (true) {

            print("> ")

            val input = readln().trim()

            if (input.equals("exit", true)) {
                println("Goodbye!")
                break
            }

            if (input.isBlank()) continue

            val result = brain.process(input)

            println("Brain : ${result.response}")

            val execution = executor.execute(result.plan)

            println("Exec  : ${execution.message}")
            println()
        }
    }
}
