package ro.greentween.copaculmeu.config;

/**
 * Cache name constants used throughout the application.
 */
public final class CacheNames {

    private CacheNames() {
        // Prevent instantiation
    }

    /** Cache for bounding box area tree queries */
    public static final String TREES_AREA = "trees-area";

    /** Cache for radial proximity / nearby tree queries */
    public static final String TREES_NEARBY = "trees-nearby";

    /** Cache for all approved trees list */
    public static final String TREES_ALL = "trees-all";

    /** Cache for individual tree details by ID */
    public static final String TREES_SINGLE = "trees-single";

    /** Cache for pending tree proposals */
    public static final String TREE_PROPOSALS = "tree-proposals";

    /** Cache for complete list of predefined tree species */
    public static final String SPECIES_ALL = "species-all";

    /** Cache for individual species by ID */
    public static final String SPECIES_SINGLE = "species-single";

    /** Cache for allowed species validation lookups */
    public static final String SPECIES_ALLOWED = "species-allowed";
}
