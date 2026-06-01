package com.energypal.console;

import com.energypal.console.command.ConsoleCommandProcessor;
import com.energypal.console.domain.PlanCostCalculator;
import com.energypal.console.io.JsonPlanLoader;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Scanner;

public class ConsoleApplication {
    public static void main(String[] args) {
        var processor = new ConsoleCommandProcessor(
                new JsonPlanLoader(),
                new PlanCostCalculator(),
                Path.of("").toAbsolutePath(),
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true)
        );

        try (var scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine() && processor.process(scanner.nextLine())) {
                // Loop until the user enters exit or stdin closes.
            }
        }
    }
}
