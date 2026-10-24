package ro.greentween.copaculmeu;

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
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesResponse;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SpeciesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should retrieve predefined species list including Magnolia x soulangeana (Magnolie) and Timisoara species")
    void shouldRetrievePredefinedSpeciesList() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(40))))
                .andExpect(jsonPath("$[*].name", hasItem("Magnolia x soulangeana (Magnolie)")))
                .andExpect(jsonPath("$[*].name", hasItem("Platanus x acerifolia (Platan)")))
                .andExpect(jsonPath("$[*].name", hasItem("Tilia tomentosa (Tei argintiu)")))
                .andExpect(jsonPath("$[*].name", hasItem("Ginkgo biloba (Ginkgo)")))
                .andReturn();

        List<?> list = objectMapper.readValue(result.getResponse().getContentAsString(), List.class);
        assertThat(list).isNotEmpty();
    }

    @Test
    @DisplayName("Should retrieve predefined species list via /api/trees/species alias endpoint")
    void shouldRetrieveSpeciesViaTreesAlias() throws Exception {
        mockMvc.perform(get("/api/trees/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(40))))
                .andExpect(jsonPath("$[*].name", hasItem("Magnolia x soulangeana (Magnolie)")));
    }

    @Test
    @DisplayName("Should retrieve single species by ID")
    void shouldRetrieveSpeciesById() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/species"))
                .andExpect(status().isOk())
                .andReturn();

        TreeSpeciesResponse[] speciesArray = objectMapper.readValue(listResult.getResponse().getContentAsString(), TreeSpeciesResponse[].class);
        assertThat(speciesArray).isNotEmpty();
        Long firstId = speciesArray[0].getId();

        mockMvc.perform(get("/api/species/" + firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(firstId.intValue())))
                .andExpect(jsonPath("$.name", is(speciesArray[0].getName())))
                .andExpect(jsonPath("$.scientificName", is(speciesArray[0].getScientificName())))
                .andExpect(jsonPath("$.commonName", is(speciesArray[0].getCommonName())));
    }

    @Test
    @DisplayName("Administrator should add new tree species to the database")
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToAddSpecies() throws Exception {
        String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String speciesName = "Cedrus libani " + uniqueSuffix + " (Cedru de Liban)";
        String scientificName = "Cedrus libani " + uniqueSuffix;
        String commonName = "Cedru de Liban " + uniqueSuffix;

        TreeSpeciesCreateRequest request = TreeSpeciesCreateRequest.builder()
                .name(speciesName)
                .scientificName(scientificName)
                .commonName(commonName)
                .aliases("Lebanon cedar")
                .build();

        mockMvc.perform(post("/api/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is(speciesName)))
                .andExpect(jsonPath("$.scientificName", is(scientificName)))
                .andExpect(jsonPath("$.commonName", is(commonName)));

        // Verify species is now in the list
        mockMvc.perform(get("/api/species"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(speciesName)));
    }

    @Test
    @DisplayName("Regular user should be forbidden from adding species (403)")
    @WithMockUser(roles = "USER")
    void shouldForbidUserFromAddingSpecies() throws Exception {
        TreeSpeciesCreateRequest request = TreeSpeciesCreateRequest.builder()
                .name("Forbidden Species")
                .scientificName("Forbidden")
                .commonName("Forbidden")
                .build();

        mockMvc.perform(post("/api/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should accept tree creation with predefined species name")
    @WithMockUser(roles = "ADMIN")
    void shouldAcceptTreeCreationWithPredefinedSpecies() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4320)
                .longitude(26.1050)
                .species("Magnolia x soulangeana (Magnolie)")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.species", is("Magnolia x soulangeana (Magnolie)")));
    }

    @Test
    @DisplayName("Should accept tree creation with 'Other' species")
    @WithMockUser(roles = "ADMIN")
    void shouldAcceptTreeCreationWithOtherSpecies() throws Exception {
        TreeCreateRequest requestOther = TreeCreateRequest.builder()
                .latitude(44.4320)
                .longitude(26.1050)
                .species("Other")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOther)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.species", is("Other")));

        TreeCreateRequest requestOtherDetail = TreeCreateRequest.builder()
                .latitude(44.4321)
                .longitude(26.1051)
                .species("Other: Jacaranda mimosifolia")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestOtherDetail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.species", is("Other: Jacaranda mimosifolia")));
    }

    @Test
    @DisplayName("Should reject tree creation with unknown species not in predefined list and not Other")
    @WithMockUser(roles = "ADMIN")
    void shouldRejectTreeCreationWithInvalidSpecies() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4320)
                .longitude(26.1050)
                .species("Fake Martian Tree")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("Invalid species")));
    }

    @Test
    @DisplayName("User tree proposal should accept predefined species and Other")
    @WithMockUser(roles = "USER")
    void shouldAcceptUserProposalWithValidSpecies() throws Exception {
        TreeCreateRequest request = TreeCreateRequest.builder()
                .latitude(44.4325)
                .longitude(26.1055)
                .species("Platanus x acerifolia (Platan)")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees/propose")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.species", is("Platanus x acerifolia (Platan)")));
    }
}
