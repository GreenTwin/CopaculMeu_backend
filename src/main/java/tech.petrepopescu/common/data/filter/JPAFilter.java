package tech.petrepopescu.common.data.filter;

import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A filter that will be used for limiting objects retrieved by the DAO
 */
public abstract class JPAFilter<T> {
    public static final int DEFAULT_LIMIT = 10_000;

    protected int limit = DEFAULT_LIMIT;
    protected int offset = 0;
    protected List<String> orderBy = new ArrayList<>();

    /**
     * Return the JPA Predicate which filters the result. This can be obtained using the helper methods from this class
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root used for building the predicate
     * @return The Predicate that was build and will be used by the DAO to filter the result
     */
    public abstract Predicate getPredicate(CriteriaBuilder criteriaBuilder, Root<T> root);

    /**
     * Set the maximum number of entries returned.
     * @param limit - The number of results returned
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * The maximum number of results that will be returned
     * @return The maximum number of results returned
     */
    public int getLimit() {
        return this.limit;
    }

    /**
     * Set the offset of the results returned
     * @param offset The offset of the results
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * The offset of the results returned
     * @return The offset of the results returned
     */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Order the result by the specified filed. Can be chained to order by multiple fields
     * @param field The field name to order by
     * @return The current JPA Filter
     */
    public JPAFilter<T> orderBy(String field) {
        if (this.orderBy == null) {
            this.orderBy = new ArrayList<>();
        }
        orderBy.add(field);
        return this;
    }

    /**
     * Order the results desc by the specified filed. Can be chained to order by multiple fields
     * @param field The field name to order by
     * @return The current JPA Filter
     */
    public JPAFilter<T> orderByDesc(String field) {
        return orderBy("-"+field);
    }

    /**
     * Build an equals predicate. Resulting SQL is similar to
     *      - SELECT * FROM [table] WHERE [field] = [value]
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @param <F> The class of the value
     * @return The predicate for 'equal' operation
     */
    protected <F> Predicate equals(CriteriaBuilder criteriaBuilder, Root<T> root, String field, F value) {
        if (value == null) return null;
        return criteriaBuilder.equal(get(root, field), value);
    }

    /**
     * Build an NOT equals predicate. Resulting SQL is similar to
     *      - SELECT * FROM [table] WHERE [field] != [value]
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @param <F> The class of the value
     * @return The predicate for 'notEqual' operation
     */
    protected <F> Predicate notEquals(CriteriaBuilder criteriaBuilder, Root<T> root, String field, F value) {
        if (value == null) return null;
        return criteriaBuilder.notEqual(get(root, field), value);
    }

    /**
     * Build a less than predicate. Resulting SQL is similar to
     *      - SELECT * FROM [table] WHERE [field] < [value]
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @return The predicate for 'notEqual' operation
     */
    protected <Y extends Comparable<? super Y>> Predicate lessThan(CriteriaBuilder criteriaBuilder, Root<T> root, String field, Y value) {
        if (value == null) return null;
        return criteriaBuilder.lessThan(get(root, field), value);
    }

    /**
     * Build a less than predicate. Resulting SQL is similar to
     *      - SELECT * FROM [table] WHERE [field] <= [value]
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @return The predicate for 'notEqual' operation
     */
    protected <Y extends Comparable<? super Y>> Predicate lessThanOrEqual(CriteriaBuilder criteriaBuilder, Root<T> root, String field, Y value) {
        if (value == null) return null;
        return criteriaBuilder.lessThanOrEqualTo(get(root, field), value);
    }

    /**
     * Build a less than predicate. Resulting SQL is similar to
     *      - SELECT * FROM [table] WHERE [field] > [value]
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @return The predicate for 'notEqual' operation
     */
    protected <Y extends Comparable<? super Y>> Predicate greaterThan(CriteriaBuilder criteriaBuilder, Root<T> root, String field, Y value) {
        if (value == null) return null;
        return criteriaBuilder.greaterThan(get(root, field), value);
    }

    /**
     * Build a less than predicate. Resulting SQL is similar to
     *      - SELECT * FROM [table] WHERE [field] >= [value]
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @return The predicate for 'notEqual' operation
     */
    protected <Y extends Comparable<? super Y>> Predicate greaterThanOrEqual(CriteriaBuilder criteriaBuilder, Root<T> root, String field, Y value) {
        if (value == null) return null;
        return criteriaBuilder.greaterThanOrEqualTo(get(root, field), value);
    }

    /**
     * Build an equals predicate that ignores case
     *      - SELECT * FROM [table] WHERE UPPER([field]) = UPPER([value])
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @return The predicate for 'equal' operation
     */
    protected Predicate equalsIgnoreCase(CriteriaBuilder criteriaBuilder, Root<T> root, String field, String value) {
        if (value == null) return null;
        return criteriaBuilder.equal(criteriaBuilder.upper(get(root, field)), value.toUpperCase());
    }

    /**
     * Build a 'like' predicate. The field value will be "[any]value[any]"
     *      - SELECT * FROM [table] WHERE [field] like '%[value]%'
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @return The predicate for 'like' operation
     */
    protected Predicate like(CriteriaBuilder criteriaBuilder, Root<T> root, String field, String value) {
        if (value == null) return null;
        return criteriaBuilder.like(get(root, field), "%" + value +"%");
    }

    /**
     * Build a 'like' predicate that ignores case. The field value will be "[any]value[any]".
     *      - SELECT * FROM [table] WHERE UPPER([field]) like UPPER('%[value]%')
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param value The field value
     * @return The predicate for 'like' operation
     */
    protected Predicate likeIgnoreCase(CriteriaBuilder criteriaBuilder, Root<T> root, String field, String value) {
        if (value == null) return null;
        return criteriaBuilder.like(criteriaBuilder.upper(get(root, field)), "%" + value.toUpperCase() +"%");
    }

    protected Predicate in(Root<T> root, String field, Object... values) {
        if (values == null || values.length == 0) return null;

        return get(root, field).in(values);
    }

    protected Predicate notIn(CriteriaBuilder builder, Root<T> root, String field, Object... values) {
        if (values == null || values.length == 0) return null;

        return builder.not(get(root, field).in(values));
    }

    /**
     * Build an OR predicate for the specified values
     *      - SELECT * FROM [table] WHERE [field] = [value0] OR [field] = [value1] OR ... OR [field] = [valuen]
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root The JPA Root needed for building the predicate
     * @param field The field name
     * @param values The field values
     * @param <F> The class of the value
     * @return The predicate for 'equal' operation
     */
    protected  <F> Predicate orPredicateBuilder(CriteriaBuilder criteriaBuilder, Root<T> root, String field, List<F> values) {
        List<Predicate> predicateList = new ArrayList<>();
        if (values != null) {
            for (F value : values) {
                Predicate predicate = criteriaBuilder.equal(get(root, field), value);
                predicateList.add(predicate);
            }
        }

        if (predicateList.isEmpty()) return null;
        return criteriaBuilder.or(predicateList.toArray(new Predicate[0]));
    }

    /**
     * Combines multiple Predicates into an AND statement. Can be used to combine predicates for different values
     * Example for retrieving entries with the name 'John' and surname 'Smith':
     *      Predicate p1 = equals(cb, root, "name", "John");
     *      Predicate p1 = equals(cb, root, "surname", "Smith");
     *      Predicate withJohnSmith = andPredicateBuilder(cb, p1, p2);
     * Resulted SQL similar to:
     *      SELECT * FROM [table] WHERE name = 'John' AND surname = 'Smith'
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param predicates The list of predicates used to build the AND statement
     * @return The resulted predicate
     */
    protected Predicate andPredicateBuilder(CriteriaBuilder criteriaBuilder, Predicate... predicates) {
        List<Predicate> predicateList = new ArrayList<>();
        for (Predicate predicate:predicates) {
            if (predicate != null) {
                predicateList.add(predicate);
            }
        }

        if (predicateList.isEmpty()) return null;
        return criteriaBuilder.and(predicateList.toArray(new Predicate[0]));
    }

    /**
     * Combines multiple Predicates into an OR statement. Can be used to combine predicates for different values
     * Example for retrieving entries with the name 'John' or surname 'Smith':
     *      Predicate p1 = equals(cb, root, "name", "John");
     *      Predicate p1 = equals(cb, root, "surname", "Smith");
     *      Predicate withJohnOrSmith = andPredicateBuilder(cb, p1, p2);
     * Resulted SQL similar to:
     *      SELECT * FROM [table] WHERE name = 'John' OR surname = 'Smith'
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param predicates The list of predicates used to build the AND statement
     * @return The resulted predicate
     */
    protected Predicate orPredicateBuilder(CriteriaBuilder criteriaBuilder, Predicate... predicates) {
        List<Predicate> predicateList = new ArrayList<>();
        for (Predicate predicate:predicates) {
            if (predicate != null) {
                predicateList.add(predicate);
            }
        }
        if (predicateList.isEmpty()) return null;
        return criteriaBuilder.or(predicateList.toArray(new Predicate[0]));
    }

    /**
     * Create the orderBy statement
     * @param criteriaBuilder The CriteriaBuilder used for building the predicate
     * @param root the JPA Root needed for building the statement
     * @return The list of Order statements used for ordering the result
     */
    public List<Order> getOrderBy(CriteriaBuilder criteriaBuilder, Root<T> root) {
        if (orderBy.isEmpty()) return new ArrayList<>();

        List<Order> orderList = new ArrayList<>();
        for (String order:orderBy) {
            if (order.startsWith("-")) {
                orderList.add(criteriaBuilder.desc(get(root, order.substring(1))));
            } else {
                orderList.add(criteriaBuilder.asc(get(root, order)));
            }
        }

        return orderList;
    }

    private <Y> Path<Y> get(Root<T> root, String fieldName) {
        String[] fields = fieldName.split("\\.");
        if (fields.length == 1) {
            return root.get(fieldName);
        } else {
            Path<Y> path = root.get(fields[0]);
            for (int count = 1; count<fields.length; count++) {
                path = path.get(fields[count]);
            }
            return path;
        }
    }
}
