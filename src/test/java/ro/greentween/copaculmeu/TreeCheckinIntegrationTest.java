package ro.greentween.copaculmeu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.greentween.copaculmeu.data.dao.TreeCheckinDao;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.dao.UserActivationCodeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.TreeCheckinRequest;
import ro.greentween.copaculmeu.data.dto.TreeCheckinResponse;
import ro.greentween.copaculmeu.data.dto.UserRegisterRequest;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.TreeStatus;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import ro.greentween.copaculmeu.data.model.Tree;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.service.JwtService;
import ro.greentween.copaculmeu.service.UserService;
import tools.jackson.databind.ObjectMapper;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TreeCheckinIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TreeDao treeDao;

    @Autowired
    private TreeCheckinDao treeCheckinDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserActivationCodeDao userActivationCodeDao;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        treeCheckinDao.deleteAll();
        treeDao.deleteAll();
        userActivationCodeDao.deleteAll();
        userDao.deleteAll();
    }

    private User createTestUser(String email, String name) {
        userService.registerUser(UserRegisterRequest.builder()
                .email(email)
                .password("Password123!")
                .name(name)
                .build());
        return userDao.findByEmail(email).orElseThrow();
    }

    private Tree createApprovedTree(double lat, double lng, String species) {
        return transactionTemplate.execute(status -> {
            Tree tree = Tree.builder()
                    .latitude(lat)
                    .longitude(lng)
                    .originalLatitude(lat)
                    .originalLongitude(lng)
                    .species(species)
                    .healthLevel(HealthLevel.GOOD)
                    .wateringLevel(WateringLevel.OPTIMAL)
                    .status(TreeStatus.APPROVED)
                    .height(7.5)
                    .age(20)
                    .trunkCircumference(95.0)
                    .locationDescription("Central Park Sector 1")
                    .build();
            return treeDao.persist(tree);
        });
    }

    @Test
    @DisplayName("Should reject check-in when user is not authenticated")
    void shouldRejectCheckinWhenUnauthenticated() throws Exception {
        Tree tree = createApprovedTree(44.4320, 26.1000, "Quercus robur");

        TreeCheckinRequest request = TreeCheckinRequest.builder()
                .latitude(44.4320)
                .longitude(26.1000)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(true)
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should accept valid check-in within 2m, update watering level, last watered timestamp, and physical stats")
    void shouldRecordValidCheckinAndAffectWateringAndStats() throws Exception {
        Tree tree = createApprovedTree(44.432000, 26.100000, "Acer pseudoplatanus");
        User user = createTestUser("maria.ionescu@example.ro", "Maria Ionescu");
        String token = jwtService.generateToken(user);

        // Coordinates approx 0.8 meters away
        double checkinLat = 44.432005;
        double checkinLng = 26.100005;

        TreeCheckinRequest request = TreeCheckinRequest.builder()
                .latitude(checkinLat)
                .longitude(checkinLng)
                .wateringLevel(WateringLevel.MODERATE)
                .watered(true)
                .healthLevel(HealthLevel.EXCELLENT)
                .height(8.2)
                .age(22)
                .trunkCircumference(98.5)
                .locationDescription("Near the park entrance fountain")
                .notes("Healthy sycamore, fresh soil watering applied")
                .build();

        MvcResult result = mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.treeId", is(tree.getId().intValue())))
                .andExpect(jsonPath("$.userId", is(user.getId().toString())))
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.deviationMeters", lessThanOrEqualTo(2.0)))
                .andExpect(jsonPath("$.wateringLevel", is("MODERATE")))
                .andExpect(jsonPath("$.watered", is(true)))
                .andExpect(jsonPath("$.tree.wateringLevel", is("MODERATE")))
                .andExpect(jsonPath("$.tree.lastWateredAt").exists())
                .andExpect(jsonPath("$.tree.healthLevel", is("EXCELLENT")))
                .andExpect(jsonPath("$.tree.height", is(8.2)))
                .andExpect(jsonPath("$.tree.trunkCircumference", is(98.5)))
                .andExpect(jsonPath("$.tree.locationDescription", is("Near the park entrance fountain")))
                .andReturn();

        TreeCheckinResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), TreeCheckinResponse.class);
        assertThat(response.getValid()).isTrue();
        assertThat(response.getDeviationMeters()).isLessThan(2.0);

        // Verify database state
        Tree updatedTree = treeDao.find(tree.getId());
        assertThat(updatedTree.getWateringLevel()).isEqualTo(WateringLevel.MODERATE);
        assertThat(updatedTree.getLastWateredAt()).isNotNull();
        assertThat(updatedTree.getHealthLevel()).isEqualTo(HealthLevel.EXCELLENT);
        assertThat(updatedTree.getHeight()).isEqualTo(8.2);
    }

    @Test
    @DisplayName("Should mark check-in as invalid when deviation exceeds 2m and not alter tree state")
    void shouldRecordInvalidCheckinWhenDeviationExceedsTwoMeters() throws Exception {
        Tree tree = createApprovedTree(44.432000, 26.100000, "Betula pendula");
        User user = createTestUser("radu.stan@example.ro", "Radu Stan");
        String token = jwtService.generateToken(user);

        // Coordinates approx 110 meters away
        double checkinLat = 44.433000;
        double checkinLng = 26.100000;

        TreeCheckinRequest request = TreeCheckinRequest.builder()
                .latitude(checkinLat)
                .longitude(checkinLng)
                .wateringLevel(WateringLevel.LOW)
                .watered(true)
                .healthLevel(HealthLevel.CRITICAL)
                .proposedSpecies("Quercus robur")
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.deviationMeters", greaterThan(2.0)))
                .andExpect(jsonPath("$.tree.species", is("Betula pendula")))
                .andExpect(jsonPath("$.tree.wateringLevel", is("OPTIMAL")))
                .andExpect(jsonPath("$.tree.healthLevel", is("GOOD")));

        // Verify tree in database did not alter
        Tree databaseTree = treeDao.find(tree.getId());
        assertThat(databaseTree.getSpecies()).isEqualTo("Betula pendula");
        assertThat(databaseTree.getWateringLevel()).isEqualTo(WateringLevel.OPTIMAL);
        assertThat(databaseTree.getHealthLevel()).isEqualTo(HealthLevel.GOOD);
        assertThat(databaseTree.getLatitude()).isEqualTo(44.432000);
        assertThat(databaseTree.getLongitude()).isEqualTo(26.100000);
    }

    @Test
    @DisplayName("Should reject check-in when proposed species is not in predefined list and not Other")
    void shouldRejectCheckinWithInvalidProposedSpecies() throws Exception {
        Tree tree = createApprovedTree(44.432000, 26.100000, "Betula pendula");
        User user = createTestUser("bogus.species@example.ro", "Bogus User");
        String token = jwtService.generateToken(user);

        TreeCheckinRequest request = TreeCheckinRequest.builder()
                .latitude(44.432000)
                .longitude(26.100000)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(false)
                .proposedSpecies("Completely Fake Tree 999")
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Invalid species")));
    }

    @Test
    @DisplayName("Should recalculate tree GPS coordinates as average of valid check-ins in past 3 months")
    void shouldRecalculateGpsCoordinatesFromValidCheckinsInPastThreeMonths() throws Exception {
        double originalLat = 44.432000;
        double originalLng = 26.100000;
        Tree tree = createApprovedTree(originalLat, originalLng, "Tilia cordata");

        User user1 = createTestUser("user1@example.ro", "User 1");
        User user2 = createTestUser("user2@example.ro", "User 2");
        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        // Checkin 1: offset by ~1.2m
        double lat1 = 44.432010;
        double lng1 = 26.100005;

        TreeCheckinRequest req1 = TreeCheckinRequest.builder()
                .latitude(lat1)
                .longitude(lng1)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(false)
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.tree.latitude", is(lat1)))
                .andExpect(jsonPath("$.tree.longitude", is(lng1)));

        // Checkin 2: at exact original coords
        double lat2 = 44.432000;
        double lng2 = 26.100000;

        TreeCheckinRequest req2 = TreeCheckinRequest.builder()
                .latitude(lat2)
                .longitude(lng2)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(false)
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valid", is(true)));

        // Verification: Average of (lat1, lng1) and (lat2, lng2)
        double expectedAvgLat = (lat1 + lat2) / 2.0;
        double expectedAvgLng = (lng1 + lng2) / 2.0;

        Tree updatedTree = treeDao.find(tree.getId());
        assertThat(updatedTree.getLatitude()).isCloseTo(expectedAvgLat, within(0.000001));
        assertThat(updatedTree.getLongitude()).isCloseTo(expectedAvgLng, within(0.000001));

        // Original coordinates must remain unshifted
        assertThat(updatedTree.getOriginalLatitude()).isEqualTo(originalLat);
        assertThat(updatedTree.getOriginalLongitude()).isEqualTo(originalLng);
    }

    @Test
    @DisplayName("Should update species if at least 5 proposals from multiple users exist and strictly more than other proposals")
    void shouldUpdateSpeciesWhenConsensusReached() throws Exception {
        Tree tree = createApprovedTree(44.432000, 26.100000, "Pinus sylvestris");

        List<User> users = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            User u = createTestUser("voter" + i + "@example.ro", "Voter " + i);
            users.add(u);
            tokens.add(jwtService.generateToken(u));
        }

        // 1 to 4 proposals for "Quercus robur" -> threshold of 5 not met yet
        for (int i = 0; i < 4; i++) {
            TreeCheckinRequest req = TreeCheckinRequest.builder()
                    .latitude(44.432000)
                    .longitude(26.100000)
                    .wateringLevel(WateringLevel.OPTIMAL)
                    .watered(false)
                    .proposedSpecies("Quercus robur")
                    .build();

            mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(i))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tree.species", is("Pinus sylvestris")));
        }

        Tree checkTree = treeDao.find(tree.getId());
        assertThat(checkTree.getSpecies()).isEqualTo("Pinus sylvestris");

        // 5th proposal for "Quercus robur" -> reaches 5 proposals, strictly more than any other -> UPDATES!
        TreeCheckinRequest req5 = TreeCheckinRequest.builder()
                .latitude(44.432000)
                .longitude(26.100000)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(false)
                .proposedSpecies("Quercus robur")
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(4))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tree.species", is("Quercus robur")));

        Tree finalTree = treeDao.find(tree.getId());
        assertThat(finalTree.getSpecies()).isEqualTo("Quercus robur");
    }

    @Test
    @DisplayName("Should not update species when proposal tie exists (not strictly more than other species)")
    void shouldNotUpdateSpeciesWhenTied() throws Exception {
        Tree tree = createApprovedTree(44.432000, 26.100000, "Unknown Species");

        List<User> users = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User u = createTestUser("tie_voter" + i + "@example.ro", "Tie Voter " + i);
            users.add(u);
            tokens.add(jwtService.generateToken(u));
        }

        // 4 proposals for "Oak" and 4 proposals for "Birch"
        for (int i = 0; i < 4; i++) {
            TreeCheckinRequest reqOak = TreeCheckinRequest.builder()
                    .latitude(44.432000)
                    .longitude(26.100000)
                    .wateringLevel(WateringLevel.OPTIMAL)
                    .watered(false)
                    .proposedSpecies("Oak")
                    .build();

            mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(i))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqOak)))
                    .andExpect(status().isCreated());

            TreeCheckinRequest reqBirch = TreeCheckinRequest.builder()
                    .latitude(44.432000)
                    .longitude(26.100000)
                    .wateringLevel(WateringLevel.OPTIMAL)
                    .watered(false)
                    .proposedSpecies("Birch")
                    .build();

            mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(4 + i))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(reqBirch)))
                    .andExpect(status().isCreated());
        }

        // Both Oak and Birch have 4 proposals. Now voter 9 proposes "Oak" (Oak has 5, Birch has 4 -> Oak reaches 5 and is ahead of Birch!)
        TreeCheckinRequest reqOak5 = TreeCheckinRequest.builder()
                .latitude(44.432000)
                .longitude(26.100000)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(false)
                .proposedSpecies("Oak")
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(8))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqOak5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tree.species", is("Oak")));

        // Now voter 10 proposes "Birch" (Birch has 5, Oak has 5 -> tied! No update to Birch)
        TreeCheckinRequest reqBirch5 = TreeCheckinRequest.builder()
                .latitude(44.432000)
                .longitude(26.100000)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(false)
                .proposedSpecies("Birch")
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.get(9))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqBirch5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tree.species", is("Oak"))); // Remains Oak because 5 == 5 tie

        Tree afterTieTree = treeDao.find(tree.getId());
        assertThat(afterTieTree.getSpecies()).isEqualTo("Oak");
    }

    @Test
    @DisplayName("Should retrieve check-in observation history for a tree")
    void shouldRetrieveCheckinHistory() throws Exception {
        Tree tree = createApprovedTree(44.432000, 26.100000, "Fraxinus excelsior");
        User user = createTestUser("history.user@example.ro", "History User");
        String token = jwtService.generateToken(user);

        TreeCheckinRequest request = TreeCheckinRequest.builder()
                .latitude(44.432000)
                .longitude(26.100000)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(true)
                .notes("History test observation")
                .build();

        mockMvc.perform(post("/api/trees/" + tree.getId() + "/check-ins")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/trees/" + tree.getId() + "/check-ins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].treeId", is(tree.getId().intValue())))
                .andExpect(jsonPath("$[0].wateringLevel", is("OPTIMAL")))
                .andExpect(jsonPath("$[0].notes", is("History test observation")));
    }
}
