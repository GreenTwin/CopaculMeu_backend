package tech.petrepopescu.common.data.dao;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tech.petrepopescu.common.data.filter.JPAFilter;

import java.util.Collections;
import java.util.List;

/**
 * Generic DAO used for doing JPA operations
 */
@Slf4j
public abstract class GenericDao<T> {
    private Class<T> classOfData;

    protected EntityManager entityManager;

    /**
     * Constructor for a DAO
     * @param classOfData - The class of the domain object that will be persisted and over which JPA operations will be performed
     *                    The class must be an {@link Entity}
     */
    protected GenericDao(Class<T> classOfData, EntityManager entityManager) {
        if (!classOfData.isAnnotationPresent(Entity.class)) {
            throw new PersistenceException("The domain class must be an Entity!");
        }
        this.classOfData = classOfData;
        this.entityManager = entityManager;
    }

    /**
     * Returns all items from the database, with the default limit for the maximum number of results
     * @return The entries from the database
     */
    public List<T> getAll() {
        return get(null);
    }

    /**
     * Returns all items from the database, with the default limit for the maximum number of results
     * @return The entries from the database
     */
    public List<T> getAllSorted(String sortBy) {
        return get(null, sortBy);
    }

    /**
     * Return the list of items that satisfy the filter provided. If the filter is null, all items will be returned.
     * @param filter The filter for used for returning the items. Can be null, in which case all items are returned
     * @return The list of items that satisfy the given filter
     */
    public List<T> get(JPAFilter<T> filter) {
        return get(filter, null);
    }

    /**
     * Return the list of items that satisfy the filter provided. If the filter is null, all items will be returned.
     * @param sortBy Sort the data by the given input parameter
     * @return The list of items that satisfy the given filter
     */
    public List<T> getSorted(String sortBy) {
        return get(null, sortBy);
    }

    /**
     * Return the list of items that satisfy the filter provided. If the filter is null, all items will be returned.
     * @param filter The filter for used for returning the items. Can be null, in which case all items are returned
     * @return The list of items that satisfy the given filter
     */
    public List<T> get(JPAFilter<T> filter, String sortBy) {
        try {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(classOfData);
            Root<T> root = criteriaQuery.from(classOfData);
            criteriaQuery.select(root);
            if (filter != null) {
                Predicate predicate = filter.getPredicate(criteriaBuilder, root);
                if (predicate != null) {
                    criteriaQuery.where(predicate);
                }
                List<Order> orderList = filter.getOrderBy(criteriaBuilder, root);
                if (!orderList.isEmpty()) {
                    criteriaQuery.orderBy(orderList);
                } else  if (StringUtils.isNotBlank(sortBy)) {
                    if (sortBy.startsWith("-")) {
                        orderList.add(criteriaBuilder.desc(root.get(sortBy)));
                    } else {
                        orderList.add(criteriaBuilder.asc(root.get(sortBy)));
                    }
                }
            }
            TypedQuery<T> query = entityManager.createQuery(criteriaQuery);
            if (filter != null) {
                query.setMaxResults(filter.getLimit()).setFirstResult(filter.getOffset());
            } else {
                query.setMaxResults(JPAFilter.DEFAULT_LIMIT).setFirstResult(0);
            }
            return query.getResultList();
        } catch (NoResultException exception) {
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to get data", e);
            throw e;
        }
    }

    /**
     * Returns the number of entries that satisfy the given filter
     * @param filter The Filter that will be applied on the results. Can be null
     * @return The number of entries
     */
    public long count(JPAFilter<T> filter) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(classOfData);
        criteriaQuery.select(criteriaBuilder.count(root));

        if (filter != null) {
            Predicate predicate = filter.getPredicate(criteriaBuilder, root);
            if (predicate != null) {
                criteriaQuery.where(predicate);
            }
        }

        try {
            TypedQuery<Long> result = entityManager.createQuery(criteriaQuery);
            return result.getSingleResult();
        }  catch (NoResultException exception) {
            return 0l;
        }
    }

    /**
     * Return the entry with the given ID or null if no such entry exists
     * @param id The ID of the object
     * @return The object with the specified ID
     */
    public T find(Object id) {
        return entityManager.find(classOfData, id);
    }

    public T reference(Object id) {
        return entityManager.getReference(classOfData, id);
    }

    /**
     * Insert the given object in the database.
     * @param obj The object to store in the database
     * @return The object stored, with the generated values set
     */
    public T persist(T obj) {
        entityManager.persist(obj);
        return obj;
    }

    /**
     * Insert the given object in the database. Same as persist();
     * @param obj The object to store in the database
     * @return The object stored, with the generated values set
     */
    public T insert(T obj) {
        return persist(obj);
    }

    /**
     * Update the entry in the database
     * @param obj The object to update
     * @return The updated object
     */
    public T merge(T obj) {
        entityManager.merge(obj);
        return obj;
    }

    /**
     * Update the entry in the database. Same as merge();
     * @param obj The object to update
     * @return The updated object
     */
    public T update(T obj) {
        return merge(obj);
    }

    /**
     * Hard deletes the object with the given ID from the database, if it exists
     * @param id The ID of the object that will be deleted
     */
    public void delete(String id) {
        T obj = entityManager.find(classOfData, id);
        if (obj != null) {
            entityManager.remove(obj);
        }
    }

    /**
     * Execute a delete statement based on the provided filter. If the filter is null or returns a null predicate,
     * the delete method is ignored.
     * @param filter The filter
     */
    public void delete(JPAFilter<T> filter) {
        if (filter == null) return;

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> criteriaDelete = criteriaBuilder.createCriteriaDelete(classOfData);
        Root<T> root = criteriaDelete.from(classOfData);

        Predicate predicate = filter.getPredicate(criteriaBuilder, root);
        if (predicate != null) {
            criteriaDelete.where(predicate);
            int result = entityManager.createQuery(criteriaDelete).executeUpdate();
            log.info("There were " + result + " items deleted");
        } else {
            log.warn("Executing a delete without any criteria. Ignoring!");
        }
    }

    /**
     * Hard deletes the object from the database
     * @param obj The object that will be deleted
     */
    public void delete(T obj) {
        entityManager.remove(entityManager.contains(obj) ? obj : entityManager.merge(obj));
    }
}
