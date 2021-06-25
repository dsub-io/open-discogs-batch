package io.dsub.discogs.batch.job.writer;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.List;

@RequiredArgsConstructor
public class CustomJpaItemWriter<T> implements ItemWriter<T>, InitializingBean {

    private final EntityManagerFactory entityManagerFactory;

    /**
     * Check mandatory properties - there must be an entityManagerFactory.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(entityManagerFactory, "An EntityManagerFactory is required");
    }

    /**
     * Merge all provided items that aren't already in the persistence context
     * and then flush the entity manager.
     *
     * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
     */
    @Override
    public void write(List<? extends T> items) {
        EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (entityManager == null) {
            throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
        }
        doWrite(entityManager, items);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Do perform the actual write operation. This can be overridden in a
     * subclass if necessary.
     *
     * @param entityManager the EntityManager to use for the operation
     * @param items         the list of items to use for the write
     */
    protected void doWrite(EntityManager entityManager, List<? extends T> items) {
        if (!items.isEmpty()) {
            for (T item : items) {
                if (!entityManager.contains(item)) {
                    entityManager.merge(item);
                }
            }
        }
    }
}
