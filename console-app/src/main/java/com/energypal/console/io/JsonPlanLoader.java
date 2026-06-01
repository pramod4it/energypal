package com.energypal.console.io;

import com.energypal.console.domain.Plan;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JsonPlanLoader implements PlanLoader {
    private final ObjectMapper objectMapper;

    public JsonPlanLoader() {
        this(new ObjectMapper());
    }

    JsonPlanLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Plan> load(Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), new TypeReference<>() {});
    }
}
