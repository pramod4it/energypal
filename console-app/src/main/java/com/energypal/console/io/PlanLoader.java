package com.energypal.console.io;

import com.energypal.console.domain.Plan;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface PlanLoader {
    List<Plan> load(Path path) throws IOException;
}
