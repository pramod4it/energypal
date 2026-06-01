package com.energypal.console.command;

import com.energypal.console.domain.PlanCostCalculator;
import com.energypal.console.io.JsonPlanLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleCommandProcessorTest {
    @TempDir
    Path tempDir;

    @Test
    void processesSampleCommandsAndPrintsExpectedCsv() throws Exception {
        Files.writeString(tempDir.resolve("plans.json"), sampleJson());
        var stdout = new StringWriter();
        var stderr = new StringWriter();
        var processor = new ConsoleCommandProcessor(
                new JsonPlanLoader(),
                new PlanCostCalculator(),
                tempDir,
                new PrintWriter(stdout, true),
                new PrintWriter(stderr, true)
        );

        assertThat(processor.process("input plans.json")).isTrue();
        assertThat(processor.process("annual_cost 1000")).isTrue();
        assertThat(processor.process("annual_cost 2000")).isTrue();
        assertThat(processor.process("exit")).isFalse();

        assertThat(stdout.toString().replace("\r\n", "\n")).isEqualTo("""
                energyOne,planOne,108.68
                energyThree,planThree,111.25
                energyTwo,planTwo,120.22
                energyFour,planFour,121.33
                energyThree,planThree,205.75
                energyOne,planOne,213.68
                energyFour,planFour,215.83
                energyTwo,planTwo,235.72
                """);
        assertThat(stderr.toString()).isEmpty();
    }

    @Test
    void resolvesPlansFromSampleFolderForAssignmentCommand() throws Exception {
        Files.createDirectories(tempDir.resolve("sample"));
        Files.writeString(tempDir.resolve("sample").resolve("plans.json"), sampleJson());
        var stdout = new StringWriter();
        var stderr = new StringWriter();
        var processor = new ConsoleCommandProcessor(
                new JsonPlanLoader(),
                new PlanCostCalculator(),
                tempDir,
                new PrintWriter(stdout, true),
                new PrintWriter(stderr, true)
        );

        assertThat(processor.process("input plans.json")).isTrue();
        assertThat(processor.process("annual_cost 1000")).isTrue();

        assertThat(stdout.toString().replace("\r\n", "\n")).startsWith("""
                energyOne,planOne,108.68
                energyThree,planThree,111.25
                """);
        assertThat(stderr.toString()).isEmpty();
    }

    @Test
    void reportsErrorsForInvalidFlowAndUnknownCommands() {
        var stdout = new StringWriter();
        var stderr = new StringWriter();
        var processor = new ConsoleCommandProcessor(
                new JsonPlanLoader(),
                new PlanCostCalculator(),
                tempDir,
                new PrintWriter(stdout, true),
                new PrintWriter(stderr, true)
        );

        assertThat(processor.process("annual_cost 1000")).isTrue();
        assertThat(processor.process("unknown")).isTrue();
        assertThat(processor.process("input missing.json")).isTrue();
        assertThat(processor.process("   ")).isTrue();

        assertThat(stdout.toString()).isEmpty();
        assertThat(stderr.toString()).contains(
                "No plans loaded. Use input <filename> first.",
                "Unsupported command: unknown",
                "Unable to load plans:"
        );
    }

    private String sampleJson() {
        return """
                [
                  {
                    "supplier_name": "energyOne",
                    "plan_name": "planOne",
                    "prices": [
                      { "rate": 13.5, "threshold": 100 },
                      { "rate": 10 }
                    ]
                  },
                  {
                    "supplier_name": "energyTwo",
                    "plan_name": "planTwo",
                    "prices": [
                      { "rate": 12.5, "threshold": 300 },
                      { "rate": 11 }
                    ]
                  },
                  {
                    "supplier_name": "energyThree",
                    "plan_name": "planThree",
                    "prices": [
                      { "rate": 14.5, "threshold": 250 },
                      { "rate": 10.1, "threshold": 200 },
                      { "rate": 9 }
                    ]
                  },
                  {
                    "supplier_name": "energyFour",
                    "plan_name": "planFour",
                    "prices": [
                      { "rate": 9 }
                    ],
                    "standing_charge": 7
                  }
                ]
                """;
    }
}
