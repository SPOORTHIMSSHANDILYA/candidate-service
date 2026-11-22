package com.recruit.candidate_service.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests: real HTTP layer (MockMvc with security and filters), real Flyway migration
 * and a real MySQL 8 database in Docker (Testcontainers). Nothing is mocked.
 * Covers what unit tests cannot: search/filter SQL, validation and error JSON, JWT security,
 * database constraints and audit fields. Docker must be running.
 */
@SpringBootTest(properties = {
        // values for the ${...} placeholders in application.yml (the real DB comes from the container)
        "DB_USERNAME=unused",
        "DB_PASSWORD=unused",
        "RECRUITER_PRIYA_PASSWORD=test-password-priya",
        "RECRUITER_ARJUN_PASSWORD=test-password-arjun",
        "JWT_SECRET=test-secret-that-is-at-least-32-characters-long",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class CandidateApiIntegrationTest {

    /** One MySQL container for the whole class; Spring Boot reads its URL/user/password (@ServiceConnection). */
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0");

    static {
        MYSQL.start();
    }

    private static final String CANDIDATES = "/api/v1/candidates";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String token;

    @BeforeEach
    void cleanDatabaseAndSignIn() throws Exception {
        jdbc.update("DELETE FROM candidates");   // skills are removed by ON DELETE CASCADE
        token = login("priya.n", "test-password-priya");
    }

    // ------------------------------------------------------------------ schema

    @Test
    void flywayCreatesTheSchema() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    // ------------------------------------------------------------------ search and filters

    @Test
    void skillsFilterWithAllRequiresEverySkill() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java", "aws");
        create("Rahul", "Nair", "rahul@example.com", "Pune", 5, "java");
        create("Sneha", "Iyer", "sneha@example.com", "Pune", 5, "python");

        search("skills=java,aws")
                .andExpect(jsonPath("$.content[*].email", contains("ananya@example.com")));
    }

    @Test
    void skillsFilterWithAnyRequiresAtLeastOneSkill() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java", "aws");
        create("Rahul", "Nair", "rahul@example.com", "Pune", 5, "java");
        create("Sneha", "Iyer", "sneha@example.com", "Pune", 5, "python");

        search("skills=aws,python&skillMatch=ANY")
                .andExpect(jsonPath("$.content[*].email",
                        containsInAnyOrder("ananya@example.com", "sneha@example.com")));
    }

    @Test
    void cityAndExperienceRangeAreCombined() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java");
        create("Rahul", "Nair", "rahul@example.com", "Pune", 10, "java");
        create("Sneha", "Iyer", "sneha@example.com", "Mumbai", 5, "java");

        search("city=Pune&minExperience=3&maxExperience=7")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("ananya@example.com"));
    }

    @Test
    void textSearchMatchesFullNameIgnoringCase() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java");
        create("Rahul", "Nair", "rahul@example.com", "Pune", 5, "java");

        searchText("ANANYA RAO")
                .andExpect(jsonPath("$.content[*].email", contains("ananya@example.com")));
    }

    @Test
    void likeWildcardsInSearchTextAreTreatedAsPlainText() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java");

        searchText("%")   // "%" must be searched as a character, not match everything
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void archivedCandidatesAreHiddenUnlessRequested() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java");
        long archivedId = create("Rahul", "Nair", "rahul@example.com", "Pune", 5, "java");
        changeStatus(archivedId, "ARCHIVED");

        search("").andExpect(jsonPath("$.content[*].email", contains("ananya@example.com")));
        search("status=ARCHIVED").andExpect(jsonPath("$.content[*].email", contains("rahul@example.com")));
    }

    @Test
    void resultsArePagedAndSorted() throws Exception {
        create("Ananya", "Charlie", "c@example.com", "Pune", 5, "java");
        create("Ananya", "Alpha", "a@example.com", "Pune", 5, "java");
        create("Ananya", "Bravo", "b@example.com", "Pune", 5, "java");

        search("page=0&size=2&sort=lastName,asc")
                .andExpect(jsonPath("$.content[*].lastName", contains("Alpha", "Bravo")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void countryOpenToWorkNoticePeriodAndSeveralStatusesAreCombined() throws Exception {
        createWith("match@example.com", "India", true, 15);
        createWith("not-open@example.com", "India", false, 15);        // excluded: not open to work
        createWith("long-notice@example.com", "India", true, 90);      // excluded: notice > 30 days
        createWith("other-country@example.com", "USA", true, 15);      // excluded: country
        long inactive = createWith("inactive@example.com", "India", true, 15);
        long archived = createWith("archived@example.com", "India", true, 15);
        changeStatus(inactive, "INACTIVE");                            // included: status requested
        changeStatus(archived, "ARCHIVED");                            // excluded: status not requested

        search("country=India&openToWork=true&maxNoticeDays=30&status=ACTIVE,INACTIVE")
                .andExpect(jsonPath("$.content[*].email",
                        containsInAnyOrder("match@example.com", "inactive@example.com")));
    }

    // ------------------------------------------------------------------ overview and skill lookup

    @Test
    void statsCountsCandidatesFromTheDatabase() throws Exception {
        createWith("active@example.com", "India", true, 15);
        long inactive = createWith("inactive@example.com", "India", true, 15);
        long archived = createWith("archived@example.com", "India", false, 15);
        changeStatus(inactive, "INACTIVE");
        changeStatus(archived, "ARCHIVED");

        mvc.perform(get(CANDIDATES + "/stats").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.countsByStatus.ACTIVE").value(1))
                .andExpect(jsonPath("$.countsByStatus.INACTIVE").value(1))
                .andExpect(jsonPath("$.countsByStatus.ARCHIVED").value(1))
                .andExpect(jsonPath("$.addedInLast7Days").value(3))
                .andExpect(jsonPath("$.openToWork").value(2));
    }

    @Test
    void skillLookupReturnsMatchingSkillsInOrder() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "javascript", "aws");
        create("Rahul", "Nair", "rahul@example.com", "Pune", 5, "Java", "python");

        mvc.perform(get("/api/v1/skills").param("prefix", "ja").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("java", "javascript")));   // distinct, lower case, sorted
    }

    // ------------------------------------------------------------------ create, update, delete

    @Test
    void createReturns201WithLocationAndRecordsTheSignedInRecruiter() throws Exception {
        long id = create("Ananya", "Rao", "Ananya@Example.com", "Pune", 5, "Java", "AWS");

        mvc.perform(get(CANDIDATES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ananya@example.com"))          // normalised
                .andExpect(jsonPath("$.skills", containsInAnyOrder("java", "aws")))   // normalised
                .andExpect(jsonPath("$.candidateCode").value(String.format("CAN-%05d", id)));

        String createdBy = jdbc.queryForObject("SELECT created_by FROM candidates WHERE id = ?", String.class, id);
        assertThat(createdBy).isEqualTo("priya.n");
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java");

        postCandidate(candidateJson("Other", "Person", "ANANYA@example.com", "Pune", 3, "java"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Candidate with this email already exists"));
    }

    @Test
    void updateWithStaleVersionIsRejected() throws Exception {
        long id = create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java");
        String update = """
                {"firstName": "Ananya", "lastName": "Rao", "email": "ananya@example.com",
                 "city": "Mumbai", "openToWork": true, "skills": ["java"], "version": 0}
                """;

        mvc.perform(put(CANDIDATES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Mumbai"))
                .andExpect(jsonPath("$.version").value(1));

        // same request again: version 0 is now stale (someone else saved in between)
        mvc.perform(put(CANDIDATES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteRemovesCandidateAndSkills() throws Exception {
        long id = create("Ananya", "Rao", "ananya@example.com", "Pune", 5, "java", "aws");

        mvc.perform(delete(CANDIDATES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get(CANDIDATES + "/" + id).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound());
        Integer skills = jdbc.queryForObject(
                "SELECT COUNT(*) FROM candidate_skills WHERE candidate_id = ?", Integer.class, id);
        assertThat(skills).isZero();
    }

    // ------------------------------------------------------------------ invalid input and errors

    @Test
    void invalidBodyReturns400WithEveryInvalidField() throws Exception {
        postCandidate("""
                {"firstName": "", "lastName": "Rao", "email": "not-an-email", "yearsExperience": 99}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body"))
                .andExpect(jsonPath("$.fields.firstName").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.yearsExperience").exists());
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mvc.perform(get(CANDIDATES + "/abc").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for 'id'"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        mvc.perform(get(CANDIDATES + "/999999").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Candidate not found"));
    }

    @Test
    void unsupportedSortFieldReturns400() throws Exception {
        mvc.perform(get(CANDIDATES + "?sort=email").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ security and logging

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mvc.perform(get(CANDIDATES))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mvc.perform(get(CANDIDATES).header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired token"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "priya.n", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void tokenFromLoginIdentifiesTheRecruiter() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("priya.n"))
                .andExpect(jsonPath("$.displayName").value("Priya Nair"));
    }

    @Test
    void everyResponseCarriesARequestId() throws Exception {
        mvc.perform(get(CANDIDATES).header(HttpHeaders.AUTHORIZATION, bearer()).header("X-Request-Id", "test-123"))
                .andExpect(header().string("X-Request-Id", "test-123"));
        mvc.perform(get(CANDIDATES))   // also on errors, generated when the caller sends none
                .andExpect(header().exists("X-Request-Id"));
    }

    // ------------------------------------------------------------------ helpers

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String bearer() {
        return "Bearer " + token;
    }

    /** Creates a candidate through the API and returns its id. */
    private long create(String firstName, String lastName, String email, String city, int years,
                        String... skills) throws Exception {
        String body = postCandidate(candidateJson(firstName, lastName, email, city, years, skills))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString(CANDIDATES + "/")))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** Creates a candidate with a given country, open-to-work flag and notice period; returns its id. */
    private long createWith(String email, String country, boolean openToWork, int noticeDays) throws Exception {
        String body = postCandidate("""
                {"firstName": "Test", "lastName": "Candidate", "email": "%s", "city": "Pune",
                 "country": "%s", "yearsExperience": 5, "noticePeriodDays": %d, "openToWork": %s,
                 "skills": ["java"]}
                """.formatted(email, country, noticeDays, openToWork))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** Changes the status of a freshly created candidate (version 0). */
    private void changeStatus(long id, String newStatus) throws Exception {
        mvc.perform(patch(CANDIDATES + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "%s", "version": 0}
                                """.formatted(newStatus)))
                .andExpect(status().isOk());
    }

    private ResultActions postCandidate(String json) throws Exception {
        return mvc.perform(post(CANDIDATES).header(HttpHeaders.AUTHORIZATION, bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String candidateJson(String firstName, String lastName, String email, String city,
                                        int years, String... skills) {
        String skillList = skills.length == 0 ? "" : "\"" + String.join("\", \"", skills) + "\"";
        return """
                {"firstName": "%s", "lastName": "%s", "email": "%s", "city": "%s", "country": "India",
                 "yearsExperience": %d, "noticePeriodDays": 30, "openToWork": true, "skills": [%s]}
                """.formatted(firstName, lastName, email, city, years, skillList);
    }

    private ResultActions searchText(String q) throws Exception {
        return mvc.perform(get(CANDIDATES).param("q", q).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());
    }

    private ResultActions search(String query) throws Exception {
        return mvc.perform(get(CANDIDATES + "?" + query).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());
    }
}
