package eu.organicity.annotation.repositories;

import eu.organicity.annotation.domain.TagDomain;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;

public interface TagDomainRepository extends CrudRepository<TagDomain, Long> {
    
    @Cacheable(value = "TagDomainCache")
    Iterable<TagDomain> findAll();
    
    @Cacheable(value = "TagDomainCache")
    TagDomain findById(Long id);
    
    @Cacheable(value = "TagDomainCache")
    TagDomain findByUrn(String Urn);
    
    @CacheEvict(value = "TagDomainCache", allEntries = true)
    <S extends TagDomain> S save(S entity);
    
    
    @CacheEvict(value = "TagDomainCache", allEntries = true)
    void delete(Long id);
    
}
