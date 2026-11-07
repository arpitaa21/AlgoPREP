package com.placementportal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: the REAL Spring context (security filters, controllers, services, JPA) running on an
 * in-memory H2 database. We call the API exactly as the React app would, using MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    // =====================================================================
    //  SECURITY: authentication (401) and authorisation (403)
    // =====================================================================

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/questions")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        mvc.perform(get("/api/questions").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void student_cannotUseAdminEndpoints_returns403() throws Exception {
        String student = registerStudent();
        mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/topics").header("Authorization", bearer(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hack\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(student)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canUseAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/dashboard").header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber());
    }

    @Test
    void registration_alwaysCreatesAStudent_evenIfRoleIsInjected() throws Exception {
        String email = "evil-" + UUID.randomUUID() + "@test.local";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Evil\",\"email\":\"" + email + "\",\"password\":\"Password1!\","
                                + "\"roles\":[\"ADMIN\"],\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.user.roles.length()").value(1));
    }

    // =====================================================================
    //  AUTH flow
    // =====================================================================

    @Test
    void register_thenLogin_thenMe() throws Exception {
        String email = "ok-" + UUID.randomUUID() + "@test.local";
        register(email, "Password1!").andExpect(status().isCreated());

        String token = JsonPath.read(login(email, "Password1!").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.local";
        register(email, "Password1!").andExpect(status().isCreated());
        register(email, "Password1!").andExpect(status().isConflict());
    }

    @Test
    void register_invalidInput_returns400WithFieldErrors() throws Exception {
        register("not-an-email", "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        String email = "wp-" + UUID.randomUUID() + "@test.local";
        register(email, "Password1!").andExpect(status().isCreated());
        login(email, "WrongPassword!").andExpect(status().isUnauthorized());
    }

    // =====================================================================
    //  FULL FLOW: admin builds a test -> student takes it -> weak topic -> recommendation
    // =====================================================================

    @Test
    void fullFlow_adminCreatesTest_studentTakesIt_weakTopicIsRecommended() throws Exception {
        String admin = adminToken();

        // ---- admin: topic + 3 MCQs (option 0 correct) + test ----
        long topicId = idOf(postJson("/api/admin/topics", admin,
                Map.of("name", "Topic-" + UUID.randomUUID())).andExpect(status().isCreated()));
        long[] questionIds = new long[3];
        long[][] optionIds = new long[3][3];
        for (int i = 0; i < 3; i++) {
            String body = postJson("/api/admin/questions", admin, Map.of(
                    "title", "MCQ " + i, "description", "pick one", "difficulty", "EASY", "type", "MCQ",
                    "topicId", topicId, "explanation", "because",
                    "options", List.of(Map.of("text", "right", "correct", true),
                            Map.of("text", "wrong A", "correct", false),
                            Map.of("text", "wrong B", "correct", false))))
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            questionIds[i] = ((Number) JsonPath.read(body, "$.id")).longValue();
            for (int o = 0; o < 3; o++) {
                optionIds[i][o] = ((Number) JsonPath.read(body, "$.options[" + o + "].id")).longValue();
            }
        }
        long testId = idOf(postJson("/api/admin/tests", admin, Map.of(
                "title", "Flow test", "description", "d", "durationMinutes", 10, "active", true,
                "questionIds", List.of(questionIds[0], questionIds[1], questionIds[2])))
                .andExpect(status().isCreated()));

        // ---- student: start the test ----
        String student = registerStudent();
        String startBody = mvc.perform(post("/api/tests/" + testId + "/start").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist())   // answers never leak
                .andExpect(jsonPath("$.secondsRemaining").value(org.hamcrest.Matchers.greaterThan(500)))
                .andReturn().getResponse().getContentAsString();
        long attemptId = ((Number) JsonPath.read(startBody, "$.attemptId")).longValue();

        // ---- student: answer everything WRONG (option 1) and submit ----
        List<Map<String, Object>> answers = List.of(
                Map.of("questionId", questionIds[0], "optionId", optionIds[0][1]),
                Map.of("questionId", questionIds[1], "optionId", optionIds[1][1]),
                Map.of("questionId", questionIds[2], "optionId", optionIds[2][1]));
        postJson("/api/attempts/" + attemptId + "/submit", student, Map.of("answers", answers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.score").value(0))
                .andExpect(jsonPath("$.summary.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.review[0].correct").value(false));

        // ---- submitting the same attempt twice is rejected ----
        postJson("/api/attempts/" + attemptId + "/submit", student, Map.of("answers", answers))
                .andExpect(status().isConflict());

        // ---- another student cannot read this attempt ----
        mvc.perform(get("/api/attempts/" + attemptId).header("Authorization", bearer(registerStudent())))
                .andExpect(status().isNotFound());

        // ---- weak-topic detection: 3 attempted, 0 correct -> weak ----
        mvc.perform(get("/api/recommendations/weak-topics").header("Authorization", bearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.topicId==" + topicId + ")].attempted").value(3))
                .andExpect(jsonPath("$[?(@.topicId==" + topicId + ")].accuracy").value(0.0));

        // ---- recommendations contain questions of that topic ----
        String recs = mvc.perform(get("/api/recommendations?limit=5").header("Authorization", bearer(student)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> topicIds = JsonPath.read(recs, "$[*].topicId");
        assertTrue(topicIds.stream().anyMatch(t -> t.longValue() == topicId), "weak topic should be recommended");

        // ---- targeted practice: answering correctly marks the question solved and shows up in stats ----
        postJson("/api/questions/" + questionIds[0] + "/practice", student, Map.of("optionId", optionIds[0][0]))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));
        mvc.perform(get("/api/solved").header("Authorization", bearer(student)))
                .andExpect(jsonPath("$[0].id").value(questionIds[0]));
    }

    @Test
    void dsaFlow_bookmark_solvedSubmission_andStats() throws Exception {
        String admin = adminToken();
        long topicId = idOf(postJson("/api/admin/topics", admin, Map.of("name", "DSA-" + UUID.randomUUID()))
                .andExpect(status().isCreated()));
        long qid = idOf(postJson("/api/admin/questions", admin, Map.of(
                "title", "Two Sum " + UUID.randomUUID(), "description", "find two numbers", "difficulty", "EASY",
                "type", "DSA", "topicId", topicId)).andExpect(status().isCreated()));

        String student = registerStudent();
        String auth = bearer(student);

        mvc.perform(post("/api/bookmarks/" + qid).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(post("/api/bookmarks/" + qid).header("Authorization", auth)).andExpect(status().isNoContent()); // idempotent
        mvc.perform(get("/api/bookmarks").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookmarked").value(true));

        postJson("/api/submissions", student, Map.of("questionId", qid, "language", "Java",
                "code", "class S {}", "status", "SOLVED", "timeTakenMinutes", 12)).andExpect(status().isCreated());

        mvc.perform(get("/api/questions?topicId=" + topicId).header("Authorization", auth))
                .andExpect(jsonPath("$.content[0].solved").value(true))
                .andExpect(jsonPath("$.content[0].bookmarked").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        String stats = mvc.perform(get("/api/stats/me").header("Authorization", auth))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(1, ((Number) JsonPath.read(stats, "$.solvedCount")).intValue());
        assertEquals(1, ((Number) JsonPath.read(stats, "$.currentStreakDays")).intValue());
        assertEquals(1, ((Number) JsonPath.read(stats, "$.solvedByDifficulty.EASY")).intValue());
        List<Integer> mine = JsonPath.read(stats, "$.topicProgress[?(@.topicId==" + topicId + ")].solved");
        assertEquals(1, mine.get(0));
    }

    @Test
    void mcqValidation_requiresExactlyOneCorrectOption() throws Exception {
        String admin = adminToken();
        long topicId = idOf(postJson("/api/admin/topics", admin, Map.of("name", "V-" + UUID.randomUUID()))
                .andExpect(status().isCreated()));
        postJson("/api/admin/questions", admin, Map.of(
                "title", "Bad MCQ", "description", "x", "difficulty", "EASY", "type", "MCQ", "topicId", topicId,
                "options", List.of(Map.of("text", "a", "correct", true), Map.of("text", "b", "correct", true))))
                .andExpect(status().isBadRequest());
    }

    // =====================================================================
    //  helpers
    // =====================================================================

    private ResultActions register(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("name", "Test User", "email", email, "password", password))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String registerStudent() throws Exception {
        String body = register("s-" + UUID.randomUUID() + "@test.local", "Password1!")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String adminToken() throws Exception {
        String body = login("admin@test.local", "Admin@123").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private ResultActions postJson(String url, String token, Object body) throws Exception {
        return mvc.perform(post(url).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }

    private long idOf(ResultActions result) throws Exception {
        String json = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
