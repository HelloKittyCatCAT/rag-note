package com.sya.ragnote.contract;

import static org.junit.jupiter.api.Assertions.*;

import com.sya.ragnote.enums.result.AuthResultEnum;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.enums.result.CommonResultEnum;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.enums.result.ResultCode;
import com.sya.ragnote.enums.result.UserResultEnum;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 验证 HTTP 契约与 API 模块一致，不启动数据库或伪造接口实现。 */
class HttpContractTest {
    private JsonNode contract() throws IOException {
        try (var stream = getClass().getResourceAsStream("/openapi/rag-note-mvp.json")) {
            assertNotNull(stream, "必须提供完整的 OpenAPI 契约资源");
            return JsonMapper.builder().build().readTree(stream);
        }
    }

    @Test
    void allThirteenOperationsHaveExplicitResponsesAndPermissions() throws IOException {
        JsonNode spec = contract();
        assertEquals("3.0.3", spec.path("openapi").asString(""));
        Set<String> ids = new HashSet<>();
        int operations = 0;
        for (var path : spec.path("paths").properties()) {
            assertFalse(path.getKey().contains("{"), "资源编号使用查询参数");
            for (var entry : path.getValue().properties()) {
                JsonNode operation = entry.getValue();
                assertTrue(ids.add(operation.path("operationId").asString("")));
                assertTrue(operation.has("security"));
                assertFalse(operation.path("x-permission").asString("").isBlank());
                assertTrue(operation.path("responses").has("200"));
                assertTrue(operation.path("responses").has("500"));
                if (Set.of("post", "delete").contains(entry.getKey())) {
                    assertTrue(
                            hasReference(
                                    operation.path("parameters"),
                                    "#/components/parameters/CsrfToken"));
                    assertTrue(operation.path("responses").has("403"));
                }
                operations++;
            }
        }
        assertEquals(13, operations);
        assertFalse(spec.path("paths").has("/api/research/ask"));
        checkReferences(spec, spec);
    }

    @Test
    void schemasMatchTheActualJavaFieldsIncludingInheritedFields() throws Exception {
        JsonNode spec = contract();
        for (var schema : spec.path("components").path("schemas").properties()) {
            String className = schema.getValue().path("x-java-class").asString("");
            if (className.isBlank()) {
                continue;
            }
            Class<?> type = Class.forName(className);
            if (type.isEnum()) {
                Set<String> values = new HashSet<>();
                for (Object value : type.getEnumConstants()) {
                    values.add(((Enum<?>) value).name());
                }
                Set<String> declared = new HashSet<>();
                schema.getValue().path("enum").forEach(value -> declared.add(value.asString("")));
                assertEquals(values, declared, className);
                continue;
            }
            Set<String> fields = new HashSet<>();
            for (Class<?> current = type;
                    current != Object.class;
                    current = current.getSuperclass()) {
                for (var field : current.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                        fields.add(field.getName());
                    }
                }
            }
            Set<String> declared = new HashSet<>();
            schema.getValue()
                    .path("properties")
                    .properties()
                    .forEach(p -> declared.add(p.getKey()));
            assertEquals(fields, declared, className);
        }
    }

    @Test
    void queryIdsAndPrivateNoteScopeCannotBeOverriddenByClientIdentity() throws IOException {
        JsonNode spec = contract();
        for (String method : Set.of("get", "delete")) {
            String path = method.equals("get") ? "/api/notes/detail" : "/api/notes";
            JsonNode parameters = spec.path("paths").path(path).path(method).path("parameters");
            assertTrue(hasReference(parameters, "#/components/parameters/NoteId"));
        }
        JsonNode question = spec.path("components").path("schemas").path("AskQuestionParam");
        assertEquals(1, question.path("properties").path("noteIds").path("minItems").asInt());
        assertEquals(20, question.path("properties").path("noteIds").path("maxItems").asInt());
        for (var schema : spec.path("components").path("schemas").properties()) {
            if (schema.getKey().endsWith("Param")) {
                JsonNode fields = schema.getValue().path("properties");
                for (String forbidden : Set.of("ownerId", "operatorId", "companyId", "role")) {
                    assertFalse(fields.has(forbidden), schema.getKey());
                }
            }
        }
    }

    @Test
    void everyBusinessErrorHasTheSameMessageAndAnHttpMapping() throws IOException {
        JsonNode spec = contract();
        JsonNode catalog = spec.path("x-error-catalog");
        Set<Integer> codes = new HashSet<>();
        for (ResultCode[] group :
                List.of(
                        AuthResultEnum.values(),
                        NoteResultEnum.values(),
                        ChatResultEnum.values(),
                        UserResultEnum.values(),
                        CommonResultEnum.values())) {
            for (ResultCode result : group) {
                codes.add(result.getCode());
                JsonNode error = catalog.path(Integer.toString(result.getCode()));
                assertEquals(result.getMsg(), error.path("message").asString(""));
                assertTrue(error.path("httpStatus").asInt() >= 400);
            }
        }
        assertEquals(codes.size(), catalog.size());
        for (var path : spec.path("paths").properties()) {
            for (var operation : path.getValue().properties()) {
                for (var response : operation.getValue().path("responses").properties()) {
                    for (JsonNode code : response.getValue().path("x-business-codes")) {
                        assertTrue(codes.contains(code.asInt()));
                        assertEquals(
                                Integer.parseInt(response.getKey()),
                                catalog.path(Integer.toString(code.asInt()))
                                        .path("httpStatus")
                                        .asInt());
                    }
                }
            }
        }
    }

    private boolean hasReference(JsonNode array, String ref) {
        for (JsonNode node : array) {
            if (ref.equals(node.path("$ref").asString(""))) {
                return true;
            }
        }
        return false;
    }

    private void checkReferences(JsonNode node, JsonNode root) {
        if (node.isObject()) {
            if (node.has("$ref")) {
                String ref = node.path("$ref").asString("");
                assertTrue(ref.startsWith("#/"));
                assertFalse(root.at(ref.substring(1)).isMissingNode(), ref);
            }
            node.properties().forEach(entry -> checkReferences(entry.getValue(), root));
        } else if (node.isArray()) {
            node.forEach(child -> checkReferences(child, root));
        }
    }
}
