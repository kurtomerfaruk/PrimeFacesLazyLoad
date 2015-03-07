/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kurtomerfaruk.primefaceslazyload.session;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.primefaces.model.SortOrder;

/**
 *
 * @author faruk
 */
public abstract class AbstractFacade<T> {

    private Class<T> entityClass;

    public AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    public void create(T entity) {
        getEntityManager().persist(entity);
    }

    public void edit(T entity) {
        getEntityManager().merge(entity);
    }

    public void remove(T entity) {
        getEntityManager().remove(getEntityManager().merge(entity));
    }

    public T find(Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public List<T> findAll() {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        return getEntityManager().createQuery(cq).getResultList();
    }

    public List<T> findRange(int[] range) {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    public int count() {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        return ((Long) q.getSingleResult()).intValue();
    }

    public List<T> loadLazy(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> myObj = cq.from(entityClass);
        cq.where(getFilterCondition(cb, myObj, filters));
        if (sortField != null) {
            if (sortOrder.equals(SortOrder.ASCENDING)) {
                cq.orderBy(cb.asc(myObj.get(sortField)));
            } else if (sortOrder.equals(SortOrder.DESCENDING)) {
                cq.orderBy(cb.desc(myObj.get(sortField)));
            }
        }
        return getEntityManager().createQuery(cq).setFirstResult(first).setMaxResults(pageSize).getResultList();
    }

    private Predicate getFilterCondition(CriteriaBuilder cb, Root<T> root, Map<String, Object> filters) {
        Predicate filterCondition = cb.conjunction();
        String wildCard = "%";
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String value = wildCard + filter.getValue() + wildCard;
            if (!filter.getValue().equals("")) {
                javax.persistence.criteria.Path<String> path = root.get(filter.getKey());// order.get(filter.getKey());
                filterCondition = cb.and(filterCondition, cb.like(path, value));

            }
        }
        return filterCondition;
    }

    public int count(Map<String, Object> filters) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<T> myObj = cq.from(entityClass);
        cq.where(getFilterCondition(cb, myObj, filters));
        cq.select(cb.count(myObj));
        return getEntityManager().createQuery(cq).getSingleResult().intValue();
    }

}
