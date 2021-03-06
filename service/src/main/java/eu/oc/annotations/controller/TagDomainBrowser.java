package eu.oc.annotations.controller;

import eu.oc.annotations.domain.Application;
import eu.oc.annotations.domain.Service;
import eu.oc.annotations.domain.Tag;
import eu.oc.annotations.domain.TagDomain;
import eu.oc.annotations.handlers.RestException;
import eu.oc.annotations.repositories.*;
import eu.oc.annotations.service.AnnotationService;
import eu.oc.annotations.service.DTOService;
import eu.oc.annotations.service.KPIService;
import eu.organicity.annotation.service.dto.ExperimentDTO;
import eu.organicity.annotation.service.dto.ServiceDTO;
import eu.organicity.annotation.service.dto.TagDTO;
import eu.organicity.annotation.service.dto.TagDomainDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class TagDomainBrowser {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagDomainBrowser.class);

    @Autowired
    TagDomainRepository tagDomainRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    ServiceRepository serviceRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    AnnotationService annotationService;

    @Autowired
    KPIService kpiService;

    @Autowired
    DTOService dtoService;

    // TAG DOMAIN METHODS-----------------------------------------------

    //Get All tagDomains
    @RequestMapping(value = {"tagDomains"}, method = RequestMethod.GET)
    public final List<TagDomainDTO> domainFindAll(Principal principal) {
        kpiService.addEvent(principal, "api:tagDomains");
        return dtoService.toTagDomainListDTO(tagDomainRepository.findAll());
    }


    //Get tagDomain
    @RequestMapping(value = {"tagDomains/{tagDomainUrn}"}, method = RequestMethod.GET)
    public final TagDomainDTO domainFindByUrn(@PathVariable("tagDomainUrn") String tagDomainUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:tagDomain", "tagDomainUrn", tagDomainUrn);
        return dtoService.toDTO(tagDomainRepository.findByUrn(tagDomainUrn));
    }

    // TAG METHODS-----------------------------------------------

    @RequestMapping(value = {"tagDomains/{tagDomainUrn}/tags"}, method = RequestMethod.GET)
    public final Set<TagDTO> domainGetTags(@PathVariable("tagDomainUrn") String tagDomainUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:tagDomainTags", "tagDomainUrn", tagDomainUrn);
        TagDomain d = tagDomainRepository.findByUrn(tagDomainUrn);
        try {
            return dtoService.toDTO(d).getTags();
        } catch (NullPointerException npe) {
            LOGGER.error(npe.getMessage(), npe);
            return new HashSet<>();
        }
    }

    @RequestMapping(value = {"tags/{tagUrn}"}, method = RequestMethod.GET)
    public final TagDTO tags(@PathVariable("tagUrn") String tagUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:tag", "tagUrn", tagUrn);
        Tag t = tagRepository.findByUrn(tagUrn);
        if (t == null) {
            throw new RestException("Tag Not Found");
        }
        return dtoService.toDTO(t);
    }

    // Services METHODS-----------------------------------------------
    @RequestMapping(value = {"services"}, method = RequestMethod.GET)
    public final List<ServiceDTO> services(Principal principal) {
        kpiService.addEvent(principal, "api:services");
        return dtoService.toServiceListDTO(serviceRepository.findAll());
    }

    @RequestMapping(value = {"services/{serviceUrn}"}, method = RequestMethod.GET)
    public final ServiceDTO services(@PathVariable("serviceUrn") String serviceUrn, Principal principal) {
        kpiService.addEvent(principal, "api:service", "serviceUrn", serviceUrn);
        Service s = serviceRepository.findByUrn(serviceUrn);
        if (s == null) {
            throw new RestException("Service Not Found");
        }
        return dtoService.toDTO(s);
    }

    @RequestMapping(value = {"services/{serviceUrn}/tagDomains"}, method = RequestMethod.GET)
    public final List<TagDomainDTO> serviceGetTagDomains(@PathVariable("serviceUrn") String serviceUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:serviceTagDomains", "serviceUrn", serviceUrn);
        Service s = serviceRepository.findByUrn(serviceUrn);
        if (s == null) {
            throw new RestException("Service Not Found");
        }
        return dtoService.toTagDomainListDTO(tagDomainRepository.findAllByService(s.getUrn()));
    }


    // Application Methods----------------------------------------------------------------------------------------------
    @RequestMapping(value = {"experiments"}, method = RequestMethod.GET)
    public final List<ExperimentDTO> experiments(Principal principal) {
        kpiService.addEvent(principal, "api:experiments");
        return dtoService.toExperimentListDTO(applicationRepository.findAll());
    }

    @RequestMapping(value = {"experiments/{experimentUrn}"}, method = RequestMethod.GET)
    public final ExperimentDTO experiments(@PathVariable("experimentUrn") String experimentUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:experiments", "experimentUrn", experimentUrn);
        Application a = applicationRepository.findByUrn(experimentUrn);
        if (a == null) {
            throw new RestException("Experiment Not Found");
        }
        return dtoService.toDTO(a);
    }

    @RequestMapping(value = {"experiments/{experimentUrn}/tagDomains"}, method = RequestMethod.GET)
    public final List<TagDomainDTO> experimentGetTagDomains(@PathVariable("experimentUrn") String experimentUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:experimentTagDomains", "experimentUrn", experimentUrn);
        Application a = applicationRepository.findByUrn(experimentUrn);
        if (a == null) {
            throw new RestException("Experiment Not Found");
        }
        return dtoService.toTagDomainListDTO(a.getTagDomains());
    }


}
