package ro.greentween.copaculmeu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import ro.greentween.copaculmeu.config.CacheNames;
import ro.greentween.copaculmeu.data.dao.TreeCheckinDao;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.dao.UserDao;
import ro.greentween.copaculmeu.data.dto.TreeCheckinRequest;
import ro.greentween.copaculmeu.data.dto.TreeCreateRequest;
import ro.greentween.copaculmeu.data.dto.TreeSpeciesCreateRequest;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.UserRole;
import ro.greentween.copaculmeu.data.enums.TreeStatus;
import ro.greentween.copaculmeu.data.enums.UserStatus;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import ro.greentween.copaculmeu.data.model.Tree;
import ro.greentween.copaculmeu.data.model.User;
import ro.greentween.copaculmeu.service.SpeciesService;
import ro.greentween.copaculmeu.service.TreeCacheEvictionService;
import ro.greentween.copaculmeu.service.TreeCheckinService;
import ro.greentween.copaculmeu.service.TreeService;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TreeCachingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private TreeCacheEvictionService treeCacheEvictionService;

    @Autowired
    private TreeService treeService;

    @Autowired
    private SpeciesService speciesService;

    @Autowired
    private TreeCheckinService treeCheckinService;

    @Autowired
    private TreeDao treeDao;

    @Autowired
    private TreeCheckinDao treeCheckinDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long testTreeId;
    private User testUser;

    @BeforeEach
    void setUp() {
        treeCacheEvictionService.clearAllCaches();

        transactionTemplate.execute(status -> {
            treeCheckinDao.deleteAll();
            treeDao.deleteAll();

            testUser = userDao.findByEmail("cache.user@example.com").orElseGet(() -> {
                User u = User.builder()
                        .email("cache.user@example.com")
                        .passwordHash("dummyhash")
                        .salt("dummysalt")
                        .hashVersion("PBKDF2_V1")
                        .name("Cache User")
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build();
                return userDao.persist(u);
            });

            Tree tree = Tree.builder()
                    .latitude(45.7537)
                    .longitude(21.2257)
                    .species("Magnolia x soulangeana (Magnolie)")
                    .healthLevel(HealthLevel.GOOD)
                    .wateringLevel(WateringLevel.OPTIMAL)
                    .status(TreeStatus.APPROVED)
                    .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                    .build();
            Tree persisted = treeDao.persist(tree);
            testTreeId = persisted.getId();
            return null;
        });

        treeCacheEvictionService.clearAllCaches();
    }

    @Test
    @DisplayName("Bounding box query should cache results in trees-area and serve from cache")
    void shouldCacheBoundingBoxQueries() throws Exception {
        Cache areaCache = cacheManager.getCache(CacheNames.TREES_AREA);
        assertThat(areaCache).isNotNull();

        String cacheKey = Double.valueOf(45.70) + ":" + Double.valueOf(45.80) + ":" + Double.valueOf(21.20) + ":" + Double.valueOf(21.30);
        assertThat(areaCache.get(cacheKey)).isNull();

        mockMvc.perform(get("/api/trees/area")
                        .param("minLat", "45.70")
                        .param("maxLat", "45.80")
                        .param("minLng", "21.20")
                        .param("maxLng", "21.30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Cache must now contain the query result
        Cache.ValueWrapper cached = areaCache.get(cacheKey);
        assertThat(cached).isNotNull();
        assertThat(cached.get()).isNotNull();
    }

    @Test
    @DisplayName("Radial query should cache results in trees-nearby")
    void shouldCacheRadialQueries() throws Exception {
        Cache nearbyCache = cacheManager.getCache(CacheNames.TREES_NEARBY);
        assertThat(nearbyCache).isNotNull();

        String cacheKey = "45.7537:21.2257:1000.0";
        assertThat(nearbyCache.get(cacheKey)).isNull();

        mockMvc.perform(get("/api/trees/nearby")
                        .param("lat", "45.7537")
                        .param("lng", "21.2257")
                        .param("radiusMeters", "1000"))
                .andExpect(status().isOk());

        assertThat(nearbyCache.get(cacheKey)).isNotNull();
    }

    @Test
    @DisplayName("All trees query and single tree query should be cached")
    void shouldCacheAllTreesAndSingleTree() throws Exception {
        Cache allTreesCache = cacheManager.getCache(CacheNames.TREES_ALL);
        Cache singleTreeCache = cacheManager.getCache(CacheNames.TREES_SINGLE);
        assertThat(allTreesCache).isNotNull();
        assertThat(singleTreeCache).isNotNull();

        assertThat(allTreesCache.get("all")).isNull();
        assertThat(singleTreeCache.get(testTreeId)).isNull();

        mockMvc.perform(get("/api/trees"))
                .andExpect(status().isOk());
        assertThat(allTreesCache.get("all")).isNotNull();

        mockMvc.perform(get("/api/trees/" + testTreeId))
                .andExpect(status().isOk());
        assertThat(singleTreeCache.get(testTreeId)).isNotNull();
    }

    @Test
    @DisplayName("Predefined species queries and validation should be cached")
    void shouldCacheSpeciesQueriesAndValidation() throws Exception {
        Cache speciesAllCache = cacheManager.getCache(CacheNames.SPECIES_ALL);
        Cache speciesAllowedCache = cacheManager.getCache(CacheNames.SPECIES_ALLOWED);
        assertThat(speciesAllCache).isNotNull();
        assertThat(speciesAllowedCache).isNotNull();

        assertThat(speciesAllCache.get("all")).isNull();

        mockMvc.perform(get("/api/species"))
                .andExpect(status().isOk());
        assertThat(speciesAllCache.get("all")).isNotNull();

        // Testing allowed species validation cache
        boolean allowed = speciesService.isSpeciesAllowed("Magnolia x soulangeana (Magnolie)");
        assertThat(allowed).isTrue();
        assertThat(speciesAllowedCache.get("magnolia x soulangeana (magnolie)")).isNotNull();
    }

    @Test
    @DisplayName("Adding a new tree (admin) should invalidate geographic and list caches")
    @WithMockUser(roles = "ADMIN")
    void shouldInvalidateCachesWhenAdminCreatesTree() throws Exception {
        Cache areaCache = cacheManager.getCache(CacheNames.TREES_AREA);
        Cache nearbyCache = cacheManager.getCache(CacheNames.TREES_NEARBY);
        Cache allTreesCache = cacheManager.getCache(CacheNames.TREES_ALL);

        // Pre-populate caches
        treeService.getTreesInBoundingBox(45.70, 45.80, 21.20, 21.30);
        treeService.getTreesInRadius(45.7537, 21.2257, 1000.0);
        treeService.getAllTrees();

        assertThat(areaCache.get("45.7:45.8:21.2:21.3")).isNotNull();
        assertThat(nearbyCache.get("45.7537:21.2257:1000.0")).isNotNull();
        assertThat(allTreesCache.get("all")).isNotNull();

        // Admin adds a new tree
        TreeCreateRequest newTreeRequest = TreeCreateRequest.builder()
                .latitude(45.7550)
                .longitude(21.2270)
                .species("Platanus x acerifolia (Platan)")
                .healthLevel(HealthLevel.GOOD)
                .build();

        mockMvc.perform(post("/api/trees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTreeRequest)))
                .andExpect(status().isCreated());

        // Caches must now be evicted
        assertThat(areaCache.get("45.7:45.8:21.2:21.3")).isNull();
        assertThat(nearbyCache.get("45.7537:21.2257:1000.0")).isNull();
        assertThat(allTreesCache.get("all")).isNull();
    }

    @Test
    @DisplayName("Approving a tree proposal should invalidate tree query caches and proposals cache")
    @WithMockUser(roles = "ADMIN")
    void shouldInvalidateCachesWhenTreeProposalApproved() throws Exception {
        // Propose a tree
        TreeCreateRequest proposalRequest = TreeCreateRequest.builder()
                .latitude(45.7580)
                .longitude(21.2290)
                .species("Ginkgo biloba (Ginkgo)")
                .healthLevel(HealthLevel.GOOD)
                .build();
        var proposed = treeService.proposeTree(proposalRequest, testUser.getId());

        // Pre-populate caches
        treeService.getPendingProposals();
        treeService.getAllTrees();
        treeService.getTreeById(proposed.getId());

        Cache proposalsCache = cacheManager.getCache(CacheNames.TREE_PROPOSALS);
        Cache allTreesCache = cacheManager.getCache(CacheNames.TREES_ALL);
        Cache singleCache = cacheManager.getCache(CacheNames.TREES_SINGLE);

        assertThat(proposalsCache.get("pending")).isNotNull();
        assertThat(allTreesCache.get("all")).isNotNull();
        assertThat(singleCache.get(proposed.getId())).isNotNull();

        // Admin approves proposal
        mockMvc.perform(post("/api/trees/" + proposed.getId() + "/approve"))
                .andExpect(status().isOk());

        // Both proposals, all trees, and single tree caches must be evicted
        assertThat(proposalsCache.get("pending")).isNull();
        assertThat(allTreesCache.get("all")).isNull();
        assertThat(singleCache.get(proposed.getId())).isNull();
    }

    @Test
    @DisplayName("Editing a tree via valid check-in should invalidate tree and area caches")
    void shouldInvalidateCachesWhenTreeIsEditedViaCheckin() {
        // Pre-populate caches for the existing tree
        treeService.getTreeById(testTreeId);
        treeService.getAllTrees();
        treeService.getTreesInRadius(45.7537, 21.2257, 1000.0);

        Cache singleCache = cacheManager.getCache(CacheNames.TREES_SINGLE);
        Cache allTreesCache = cacheManager.getCache(CacheNames.TREES_ALL);
        Cache nearbyCache = cacheManager.getCache(CacheNames.TREES_NEARBY);

        assertThat(singleCache.get(testTreeId)).isNotNull();
        assertThat(allTreesCache.get("all")).isNotNull();
        assertThat(nearbyCache.get("45.7537:21.2257:1000.0")).isNotNull();

        // Perform a valid checkin modifying the tree (watered, health update)
        TreeCheckinRequest checkinRequest = TreeCheckinRequest.builder()
                .latitude(45.7537)
                .longitude(21.2257)
                .wateringLevel(WateringLevel.OPTIMAL)
                .watered(true)
                .healthLevel(HealthLevel.EXCELLENT)
                .build();

        treeCheckinService.checkIn(testTreeId, checkinRequest, testUser.getId());

        // All caches covering this tree must be evicted
        assertThat(singleCache.get(testTreeId)).isNull();
        assertThat(allTreesCache.get("all")).isNull();
        assertThat(nearbyCache.get("45.7537:21.2257:1000.0")).isNull();
    }

    @Test
    @DisplayName("Adding a new species should invalidate species list and validation caches")
    @WithMockUser(roles = "ADMIN")
    void shouldInvalidateSpeciesCachesWhenNewSpeciesAdded() throws Exception {
        Cache speciesAllCache = cacheManager.getCache(CacheNames.SPECIES_ALL);
        Cache speciesAllowedCache = cacheManager.getCache(CacheNames.SPECIES_ALLOWED);

        // Pre-populate caches
        speciesService.getAllSpecies();
        speciesService.isSpeciesAllowed("Tilia tomentosa (Tei argintiu)");

        assertThat(speciesAllCache.get("all")).isNotNull();
        assertThat(speciesAllowedCache.get("tilia tomentosa (tei argintiu)")).isNotNull();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        TreeSpeciesCreateRequest request = TreeSpeciesCreateRequest.builder()
                .name("Acer rubrum " + uniqueSuffix + " (Artar rosu)")
                .scientificName("Acer rubrum " + uniqueSuffix)
                .commonName("Artar rosu " + uniqueSuffix)
                .build();

        mockMvc.perform(post("/api/species")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Species caches must now be evicted
        assertThat(speciesAllCache.get("all")).isNull();
        assertThat(speciesAllowedCache.get("tilia tomentosa (tei argintiu)")).isNull();
    }
}
