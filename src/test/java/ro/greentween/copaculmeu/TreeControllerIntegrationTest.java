package ro.greentween.copaculmeu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

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

    @BeforeEach
    void setUp() {
        treeDao.deleteAll();
    }

    @Test
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
                .andExpect(jsonPath("$.height", is(8.5)))
                .andExpect(jsonPath("$.age", is(25)))
                .andExpect(jsonPath("$.trunkCircumference", is(110.0)))
                .andExpect(jsonPath("$.locationDescription", is("Cismigiu Park, near the central lake")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldFailCreatingTreeWithInvalidCoordinates() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(150.0) // Invalid: latitude must be between -90 and 90
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
    void shouldFailCreatingTreeWhenRequiredFieldsAreMissing() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4323)
                // missing longitude, species, healthLevel, wateringLevel
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
    void shouldCreateTreeWithOnlyMandatoryFields() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4323)
                .longitude(26.1063)
                .species("Acer campestre")
                .healthLevel(HealthLevel.EXCELLENT)
                // all optional fields omitted
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
                .andExpect(jsonPath("$.trunkCircumference").doesNotExist());
    }

    @Test
    void shouldFindTreesWithinBoundingBox() throws Exception {
        // Tree 1: Inside Bucharest center (44.435, 26.100)
        TreeCreateRequest tree1 = TreeCreateRequest.builder()
                .latitude(44.4350)
                .longitude(26.1000)
                .species("Tilia cordata")
                .healthLevel(HealthLevel.EXCELLENT)
                .wateringLevel(WateringLevel.OPTIMAL)
                .build();

        // Tree 2: Inside Bucharest center (44.4380, 26.1050)
        TreeCreateRequest tree2 = TreeCreateRequest.builder()
                .latitude(44.4380)
                .longitude(26.1050)
                .species("Acer platanoides")
                .healthLevel(HealthLevel.GOOD)
                .wateringLevel(WateringLevel.MODERATE)
                .build();

        // Tree 3: Far away in Cluj-Napoca (46.7712, 23.6236)
        TreeCreateRequest tree3 = TreeCreateRequest.builder()
                .latitude(46.7712)
                .longitude(23.6236)
                .species("Pinus sylvestris")
                .healthLevel(HealthLevel.FAIR)
                .wateringLevel(WateringLevel.LOW)
                .build();

        mockMvc.perform(post("/api/trees").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tree1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/trees").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tree2))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/trees").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tree3))).andExpect(status().isCreated());

        // Bounding box for Bucharest center: minLat=44.430, maxLat=44.440, minLng=26.090, maxLng=26.110
        mockMvc.perform(get("/api/trees")
                        .param("minLat", "44.430")
                        .param("maxLat", "44.440")
                        .param("minLng", "26.090")
                        .param("maxLng", "26.110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].species", containsInAnyOrder("Tilia cordata", "Acer platanoides")));

        // Also test the explicit /api/trees/area endpoint
        mockMvc.perform(get("/api/trees/area")
                        .param("minLat", "44.430")
                        .param("maxLat", "44.440")
                        .param("minLng", "26.090")
                        .param("maxLng", "26.110"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldFindTreesWithinRadius() throws Exception {
        // Center: 44.4320, 26.1000
        // Tree 1: approx 150m away (44.4330, 26.1000)
        TreeCreateRequest tree1 = TreeCreateRequest.builder()
                .latitude(44.4330)
                .longitude(26.1000)
                .species("Platanus x hispanica")
                .healthLevel(HealthLevel.EXCELLENT)
                .wateringLevel(WateringLevel.OPTIMAL)
                .build();

        // Tree 2: approx 5km away (44.4700, 26.1000)
        TreeCreateRequest tree2 = TreeCreateRequest.builder()
                .latitude(44.4700)
                .longitude(26.1000)
                .species("Fraxinus excelsior")
                .healthLevel(HealthLevel.GOOD)
                .wateringLevel(WateringLevel.MODERATE)
                .build();

        mockMvc.perform(post("/api/trees").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tree1))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/trees").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tree2))).andExpect(status().isCreated());

        // Search with 500m radius
        mockMvc.perform(get("/api/trees/nearby")
                        .param("lat", "44.4320")
                        .param("lng", "26.1000")
                        .param("radiusMeters", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].species", is("Platanus x hispanica")));
    }

    @Test
    void shouldExposeOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("Copacul Meu - Tree Mapping API")))
                .andExpect(jsonPath("$.paths['/api/trees']").exists())
                .andExpect(jsonPath("$.paths['/api/trees/area']").exists())
                .andExpect(jsonPath("$.paths['/api/trees/nearby']").exists());
    }

    @Test
    void shouldExposeScalarDocumentationUI() throws Exception {
        mockMvc.perform(get("/scalar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Scalar")));
    }
}
