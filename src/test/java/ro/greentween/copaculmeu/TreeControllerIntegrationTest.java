package ro.greentween.copaculmeu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeRejectRequest;
import ro.greentween.copaculmeu.data.dto.TreeResponse;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.TreeStatus;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TreeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TreeDao treeDao;

    @Autowired
    private ro.greentween.copaculmeu.service.TreeCacheEvictionService treeCacheEvictionService;

    @BeforeEach
    void setUp() {
        treeDao.deleteAll();
        treeCacheEvictionService.clearAllCaches();
    }

    @Test
    @DisplayName("Administrator should create tree successfully with APPROVED status")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateTreeSuccessfully() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4323)
                .longitude(26.1063)
                .species("Quercus robur")
                .healthLevel(HealthLevel.GOOD)
                .wateringLevel(WateringLevel.OPTIMAL)
                .lastWateredAt(LocalDateTime.now())
                .height(8.5)
                .age(25)
                .trunkCircumference(110.0)
                .locationDescription("Cismigiu Park, near the central lake")
                .notes("Healthy oak, recently checked by forestry department")
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.latitude", is(44.4323)))
                .andExpect(jsonPath("$.longitude", is(26.1063)))
                .andExpect(jsonPath("$.species", is("Quercus robur")))
                .andExpect(jsonPath("$.healthLevel", is("GOOD")))
                .andExpect(jsonPath("$.wateringLevel", is("OPTIMAL")))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.height", is(8.5)))
                .andExpect(jsonPath("$.age", is(25)))
                .andExpect(jsonPath("$.trunkCircumference", is(110.0)))
                .andExpect(jsonPath("$.locationDescription", is("Cismigiu Park, near the central lake")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("Should reject tree creation for unauthenticated user")
    void shouldRejectTreeCreationForUnauthenticatedUser() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4323)
                .longitude(26.1063)
                .species("Quercus robur")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject tree creation for regular user (403 Forbidden)")
    @WithMockUser(roles = "USER")
    void shouldRejectTreeCreationForRegularUser() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4323)
                .longitude(26.1063)
                .species("Quercus robur")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

//    TODO: Temporary, all trees added are approved until we test the app and populate the database better
//
//    @Test
//    @DisplayName("Regular user should propose tree with PENDING_APPROVAL status")
//    @WithMockUser(roles = "USER")
//    void shouldAllowUserToProposeTree() throws Exception {
//        TreeCreateRequest request = TreeCreateRequest.builder()
//                .latitude(44.4350)
//                .longitude(26.1020)
//                .species("Tilia cordata")
//                .healthLevel(HealthLevel.FAIR)
//                .wateringLevel(WateringLevel.LOW)
//                .locationDescription("University Square")
//                .build();
//
//        MvcResult result = mockMvc.perform(post("/api/trees/propose")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.id").isNumber())
//                .andExpect(jsonPath("$.species", is("Tilia cordata")))
//                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")))
//                .andReturn();
//
//        TreeResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), TreeResponse.class);
//
//        // Pending proposal must NOT appear in public queries
//        mockMvc.perform(get("/api/trees"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(0)));
//
//        mockMvc.perform(get("/api/trees/area")
//                        .param("minLat", "44.4300")
//                        .param("maxLat", "44.4400")
//                        .param("minLng", "26.1000")
//                        .param("maxLng", "26.1100"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$", hasSize(0)));
//    }

    @Test
    @DisplayName("Administrator should list proposals, approve, and make tree visible on public map")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToApproveProposal() throws Exception {
        // 1. Propose tree
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4360)
                .longitude(26.1040)
                .species("Platanus acerifolia")
                .healthLevel(HealthLevel.EXCELLENT)
                .build();

        MvcResult proposalResult = mockMvc.perform(post("/api/trees/propose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TreeResponse proposal = objectMapper.readValue(proposalResult.getResponse().getContentAsString(), TreeResponse.class);
        Long treeId = proposal.getId();

        // 2. Admin queries pending proposals
        mockMvc.perform(get("/api/trees/proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(treeId.intValue())))
                .andExpect(jsonPath("$[0].status", is("PENDING_APPROVAL")));

        // 3. Admin approves proposal
        mockMvc.perform(post("/api/trees/" + treeId + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(treeId.intValue())))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.reviewedAt").exists());

        // 4. Now tree appears in public queries
        mockMvc.perform(get("/api/trees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].species", is("Platanus acerifolia")))
                .andExpect(jsonPath("$[0].status", is("APPROVED")));
    }

    @Test
    @DisplayName("Administrator should reject proposal with reason")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToRejectProposal() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4310)
                .longitude(26.1080)
                .species("Populus nigra")
                .healthLevel(HealthLevel.POOR)
                .build();

        MvcResult proposalResult = mockMvc.perform(post("/api/trees/propose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TreeResponse proposal = objectMapper.readValue(proposalResult.getResponse().getContentAsString(), TreeResponse.class);
        Long treeId = proposal.getId();

        TreeRejectRequest rejectRequest = TreeRejectRequest.builder()
                .reason("Duplicate entry or inaccurate coordinates")
                .build();

        // Reject proposal
        mockMvc.perform(post("/api/trees/" + treeId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(treeId.intValue())))
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.rejectionReason", is("Duplicate entry or inaccurate coordinates")));

        // Rejected tree does NOT appear on public map
        mockMvc.perform(get("/api/trees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Fail creating tree with invalid coordinates")
    @WithMockUser(roles = "ADMIN")
    void shouldFailCreatingTreeWithInvalidCoordinates() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(150.0)
                .longitude(26.1063)
                .species("Betula pendula")
                .healthLevel(HealthLevel.FAIR)
                .wateringLevel(WateringLevel.LOW)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.latitude").exists());
    }

    @Test
    @DisplayName("Fail creating tree when required fields are missing")
    @WithMockUser(roles = "ADMIN")
    void shouldFailCreatingTreeWhenRequiredFieldsAreMissing() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4323)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.longitude").exists())
                .andExpect(jsonPath("$.errors.species").exists())
                .andExpect(jsonPath("$.errors.healthLevel").exists())
                .andExpect(jsonPath("$.errors.wateringLevel").doesNotExist());
    }

    @Test
    @DisplayName("Create tree with only mandatory fields")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateTreeWithOnlyMandatoryFields() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4323)
                .longitude(26.1063)
                .species("Acer campestre")
                .healthLevel(HealthLevel.EXCELLENT)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.latitude", is(44.4323)))
                .andExpect(jsonPath("$.longitude", is(26.1063)))
                .andExpect(jsonPath("$.species", is("Acer campestre")))
                .andExpect(jsonPath("$.healthLevel", is("EXCELLENT")))
                .andExpect(jsonPath("$.wateringLevel").doesNotExist())
                .andExpect(jsonPath("$.height").doesNotExist())
                .andExpect(jsonPath("$.age").doesNotExist())
                .andExpect(jsonPath("$.trunkCircumference").doesNotExist())
                .andExpect(jsonPath("$.locationDescription").doesNotExist())
                .andExpect(jsonPath("$.notes").doesNotExist());
    }
}
