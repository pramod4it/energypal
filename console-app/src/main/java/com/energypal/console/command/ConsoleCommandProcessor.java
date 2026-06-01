package com.energypal.console.command;

import com.energypal.console.domain.Plan;
import com.energypal.console.domain.PlanCostCalculator;
import com.energypal.console.io.PlanLoader;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConsoleCommandProcessor {
    private final PlanLoader planLoader;
    private final PlanCostCalculator calculator;
    private final Path workingDirectory;
    private final PrintWriter output;
    private final PrintWriter errorOutput;
    private List<Plan> plans = List.of();

    public ConsoleCommandProcessor(
            PlanLoader planLoader,
            PlanCostCalculator calculator,
            Path workingDirectory,
            PrintWriter output,
            PrintWriter errorOutput
    ) {
        this.planLoader = planLoader;
        this.calculator = calculator;
        this.workingDirectory = workingDirectory;
        this.output = output;
        this.errorOutput = errorOutput;
    }

    public boolean process(String commandLine) {
        var tokens = commandLine.trim().split("\\s+", 2);
        var command = tokens.length == 0 ? "" : tokens[0];
        var argument = tokens.length == 2 ? tokens[1].trim() : "";

        return switch (command) {
            case "input" -> loadPlans(argument);
            case "annual_cost" -> printAnnualCosts(argument);
            case "exit" -> false;
            case "" -> true;
            default -> reportError("Unsupported command: " + command);
        };
    }

    private boolean loadPlans(String filename) {
        try {
            plans = planLoader.load(resolve(filename));
        } catch (IOException exception) {
            errorOutput.println("Unable to load plans: " + exception.getMessage());
        }
        return true;
    }

    private boolean printAnnualCosts(String annualConsumption) {
        if (plans.isEmpty()) {
            return reportError("No plans loaded. Use input <filename> first.");
        }

        calculator.annualCosts(plans, new BigDecimal(annualConsumption))
                .forEach(planCost -> output.println(planCost.toCsvLine()));
        return true;
    }

    private Path resolve(String filename) {
        var path = Path.of(filename);
        if (path.isAbsolute()) {
            return path;
        }

        var candidates = List.of(
                workingDirectory.resolve(path).normalize(),
                workingDirectory.resolve("sample").resolve(path).normalize(),
                workingDirectory.resolve("console-app").resolve("sample").resolve(path).normalize()
        );
        return candidates.stream()
                .filter(Files::exists)
                .findFirst()
                .orElse(candidates.get(0));
    }

    private boolean reportError(String message) {
        errorOutput.println(message);
        return true;
    }
}
