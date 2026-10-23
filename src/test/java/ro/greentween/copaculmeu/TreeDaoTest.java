package ro.greentween.copaculmeu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ro.greentween.copaculmeu.data.dao.TreeDao;
import ro.greentween.copaculmeu.data.enums.HealthLevel;
import ro.greentween.copaculmeu.data.enums.WateringLevel;
import ro.greentween.copaculmeu.data.filter.TreeFilter;
import ro.greentween.copaculmeu.data.model.Tree;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class TreeDaoTest {

    @Autowired
    private TreeDao treeDao;

    @BeforeEach
    void setUp() {
        treeDao.deleteAll();
    }

    @Test
    void shouldPersistAndFindById() {
        Tree tree = Tree.builder()
                .latitude(44.4320)
                .longitude(26.1050)
                .species("Quercus robur")
                .healthLevel(HealthLevel.GOOD)
                .wateringLevel(WateringLevel.OPTIMAL)
                .height(10.5)
                .age(30)
                .build();

        Tree saved = treeDao.persist(tree);
        assertThat(saved.getId()).isNotNull();

        Tree found = treeDao.find(saved.getId());
        assertThat(found).isNotNull();
        assertThat(found.getSpecies()).isEqualTo("Quercus robur");
        assertThat(found.getHealthLevel()).isEqualTo(HealthLevel.GOOD);
    }

    @Test
    void shouldFilterTreesUsingTreeFilter() {
        Tree oak = treeDao.persist(Tree.builder()
                .latitude(44.4350)
                .longitude(26.1000)
                .species("Quercus robur")
                .healthLevel(HealthLevel.GOOD)
                .wateringLevel(WateringLevel.OPTIMAL)
                .build());

        Tree linden = treeDao.persist(Tree.builder()
                .latitude(44.4360)
                .longitude(26.1020)
                .species("Tilia cordata")
                .healthLevel(HealthLevel.POOR)
                .wateringLevel(WateringLevel.LOW)
                .build());

        Tree birch = treeDao.persist(Tree.builder()
                .latitude(46.7700)
                .longitude(23.6000)
                .species("Betula pendula")
                .healthLevel(HealthLevel.GOOD)
                .wateringLevel(WateringLevel.OPTIMAL)
                .build());

        // Bounding box filter (Bucharest only)
        List<Tree> inBucharest = treeDao.findInBoundingBox(44.430, 44.440, 26.090, 26.110);
        assertThat(inBucharest).hasSize(2);
        assertThat(inBucharest).extracting(Tree::getSpecies).containsExactly("Quercus robur", "Tilia cordata");

        // Filter by species
        List<Tree> oaks = treeDao.findBySpecies("Quercus robur");
        assertThat(oaks).hasSize(1);
        assertThat(oaks.get(0).getId()).isEqualTo(oak.getId());

        // Filter by health level
        List<Tree> poorTrees = treeDao.findByHealthLevel(HealthLevel.POOR);
        assertThat(poorTrees).hasSize(1);
        assertThat(poorTrees.get(0).getId()).isEqualTo(linden.getId());

        // Filter by watering level
        List<Tree> optimalTrees = treeDao.findByWateringLevel(WateringLevel.OPTIMAL);
        assertThat(optimalTrees).hasSize(2);

        // Combined custom filter using TreeFilter directly
        TreeFilter filter = new TreeFilter();
        filter.setMinLatitude(44.430);
        filter.setMaxLatitude(44.440);
        filter.setHealthLevel(HealthLevel.GOOD);
        List<Tree> filtered = treeDao.get(filter);
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).getSpecies()).isEqualTo("Quercus robur");

        // Count with filter
        long count = treeDao.count(filter);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldFindTreesWithinRadius() {
        // Center: 44.4320, 26.1000
        // Near (~110m north)
        Tree nearbyTree = treeDao.persist(Tree.builder()
                .latitude(44.4330)
                .longitude(26.1000)
                .species("Acer campestre")
                .healthLevel(HealthLevel.EXCELLENT)
                .wateringLevel(WateringLevel.OPTIMAL)
                .build());

        // Far (~5km away)
        Tree farTree = treeDao.persist(Tree.builder()
                .latitude(44.4700)
                .longitude(26.1000)
                .species("Salix alba")
                .healthLevel(HealthLevel.FAIR)
                .wateringLevel(WateringLevel.MODERATE)
                .build());

        List<Tree> radiusResults = treeDao.findTreesWithinRadius(44.4320, 26.1000, 500.0);
        assertThat(radiusResults).hasSize(1);
        assertThat(radiusResults.get(0).getSpecies()).isEqualTo("Acer campestre");
    }
}
