package eu.oc.annotations.controller;

import eu.oc.annotations.config.OrganicityAccount;
import eu.oc.annotations.domain.Application;
import eu.oc.annotations.domain.Service;
import eu.oc.annotations.domain.Tag;
import eu.oc.annotations.domain.TagDomain;
import eu.oc.annotations.handlers.RestException;
import eu.oc.annotations.repositories.ApplicationRepository;
import eu.oc.annotations.repositories.ServiceRepository;
import eu.oc.annotations.repositories.TagDomainRepository;
import eu.oc.annotations.repositories.TagRepository;
import eu.oc.annotations.service.DTOService;
import eu.oc.annotations.service.KPIService;
import eu.oc.annotations.service.OrganicityUserDetailsService;
import eu.organicity.annotation.service.dto.ExperimentDTO;
import eu.organicity.annotation.service.dto.TagDTO;
import eu.organicity.annotation.service.dto.TagDomainDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class TagDomainManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganicityAccount.class);

    @Autowired
    TagDomainRepository tagDomainRepository;

    @Autowired
    TagRepository tagRepository;

    @Autowired
    ServiceRepository serviceRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    KPIService kpiService;

    @Autowired
    DTOService dtoService;

    // TAG DOMAIN METHODS--------------------------------------------------------------------------------

    //Create tagDomain
    @RequestMapping(value = {"admin/tagDomains"}, method = RequestMethod.POST)
    public final TagDomainDTO domainCreate(@RequestBody TagDomainDTO dto, Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains", "tagDomainUrn", dto.getUrn());

        LOGGER.info("POST domainCreate");

        TagDomain a = tagDomainRepository.findByUrn(dto.getUrn());
        if (a != null) { //tagDomain Create
            LOGGER.error("TagDomain Exception: duplicate urn");
            throw new RestException("TagDomain Exception: duplicate urn");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }

        TagDomain domain = new TagDomain();
        domain.setUrn(dto.getUrn());
        domain.setDescription(dto.getDescription());
        domain.setTags(new ArrayList<>());
        try {
            domain = tagDomainRepository.save(domain);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RestException(e.getMessage());
        }

        for (TagDTO tagDTO : dto.getTags()) {
            addTag2Domain(domain.getUrn(), tagDTO);
        }

        domain = tagDomainRepository.findByUrn(dto.getUrn());
        return dtoService.toDTO(domain);
    }

    //Update tagDomain
    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}"}, method = RequestMethod.POST)
    public final TagDomainDTO domainUpdate(@PathVariable("tagDomainUrn") String tagDomainUrn, @RequestBody TagDomainDTO domain
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/update", "tagDomainUrn", domain.getUrn());

        LOGGER.info("POST domainUpdate");

        TagDomain d = tagDomainRepository.findByUrn(tagDomainUrn);
        if (d == null) {
            LOGGER.error("TagDomain Not Found");
            throw new RestException("TagDomain Not Found");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        if (ou.isTheOnlyExperimnterUsingTagDomain(applicationRepository.findApplicationsUsingTagDomain(tagDomainUrn))) {
            LOGGER.error("TagDomain is used also from other experiments. Not possible to delete/update");
            throw new RestException("TagDomain is used also from other experiments. Not possible to delete/update");
        }
        d.setDescription(domain.getDescription());
        try {
            d = tagDomainRepository.save(d);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RestException(e.getMessage());
        }
        return dtoService.toDTO(d);
    }

    //Delete tagDomain
    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}"}, method = RequestMethod.DELETE)
    public final void domainDelete(@PathVariable("tagDomainUrn") String tagDomainUrn, Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/delete", "tagDomainUrn", tagDomainUrn);

        LOGGER.info("DELETE domainDelete");

        TagDomain d = tagDomainRepository.findByUrn(tagDomainUrn);
        if (d == null) {
            LOGGER.error("TagDomain Not Found");
            throw new RestException("TagDomain Not Found");
        }
        if (d.getTags() != null && d.getTags().size() > 0) {
            LOGGER.error("TagDomain is not empty");
            throw new RestException("TagDomain is not empty");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        if (ou.isTheOnlyExperimnterUsingTagDomain(applicationRepository.findApplicationsUsingTagDomain(tagDomainUrn))) {
            LOGGER.error("TagDomain is used also from other experiments. Not possible to delete/update");
            throw new RestException("TagDomain is used also from other experiments. Not possible to delete/update");
        }
        try {
            tagDomainRepository.delete(d);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(e.getMessage());
        }
    }


    // TAG METHODS-----------------------------------------------------------------------------------------------------

    //Add tag to domain
    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}/tag"}, method = RequestMethod.POST)
    public final TagDTO domainCreateTag(@PathVariable("tagDomainUrn") String tagDomainUrn, @RequestBody TagDTO tag
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/tags/add"
                , "tagDomainUrn", tagDomainUrn
                , "tagUrn", tag.getUrn()
        );

        LOGGER.info("POST domainCreateTag");
        Tag addedTag = addTag2Domain(tagDomainUrn, tag);
        return dtoService.toDTO(addedTag);
    }

    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}/tags"}, method = RequestMethod.POST)
    public final Set<TagDTO> domainCreateTags(@PathVariable("tagDomainUrn") String tagDomainUrn, @RequestBody List<TagDTO> tags
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/tags/add"
                , "tagDomainUrn", tagDomainUrn
                , "tags", tags
        );

        LOGGER.info("POST domainCreateTags");
        Set<Tag> addedTags = new HashSet<>();
        for (final TagDTO tag : tags) {
            addedTags.add(addTag2Domain(tagDomainUrn, tag));
        }
        return dtoService.toTagSetDTO(addedTags);
    }

    //delete Tag from TagDomain
    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}/tags"}, method = RequestMethod.DELETE)
    public final void domainRemoveTag(@PathVariable("tagDomainUrn") String tagDomainUrn, @RequestBody String tagUrnList
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/tags/delete"
                , "tagDomainUrn", tagDomainUrn
                , "tagUrn", tagUrnList
        );

        LOGGER.info("DELETE domainRemoveTag " + tagUrnList);

        for (String tagUrn : tagUrnList.split(",")) {

            TagDomain d = tagDomainRepository.findByUrn(tagDomainUrn);
            if (d == null) {
                throw new RestException("TagDomain Not Found");
            }
            tagUrn = tagUrn.replace("\"", "");
            Tag t = tagRepository.findByUrn(tagUrn);
            if (t == null) {
                throw new RestException("Tag Not Found");
            }
            if (!d.containsTag(tagUrn)) {
                throw new RestException("TagDomain/Tag are not associated");
            }
            OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
            if (!ou.isAdministrator() && !ou.isExperimenter()) {
                LOGGER.error("Not Authorized Access");
                throw new RestException("Not Authorized Access");
            }
            if (ou.isTheOnlyExperimnterUsingTagDomain(applicationRepository.findApplicationsUsingTagDomain(tagDomainUrn))) {
                throw new RestException("TagDomain is used also from other experiments. Not possible to delete/update");
            }
            try {
                tagRepository.delete(t.getId());
                TagDomain domain = tagDomainRepository.findByUrn(tagDomainUrn);
                domain.getTags().remove(t);
                tagDomainRepository.save(domain);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RestException(e.getMessage());
            }
        }

    }

    //get Services using this TagDomain
    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}/services"}, method = RequestMethod.GET)
    public final List<Service> tagDomainGetServices(@PathVariable("tagDomainUrn") String tagDomainUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/services", "tagDomainUrn", tagDomainUrn);

        LOGGER.info("GET tagDomainGetServices");

        TagDomain a = tagDomainRepository.findByUrn(tagDomainUrn);
        if (a == null) {
            throw new RestException("TagDomain Not Found");
        }
        return a.getServices();
    }

    //Associate TagDomain with Service
    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}/services"}, method = RequestMethod.POST)
    public final TagDomain serviceAddTagDomains(@RequestParam(value = "serviceUrn", required = true) String serviceUrn, @PathVariable("tagDomainUrn") String tagDomainUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/services/add"
                , "tagDomainUrn", tagDomainUrn
                , "serviceUrn", serviceUrn
        );

        LOGGER.info("POST serviceAddTagDomains");

        Service s = serviceRepository.findByUrn(serviceUrn);
        if (s == null) {
            throw new RestException("Service Not Found");
        }
        TagDomain a = tagDomainRepository.findByUrn(tagDomainUrn);
        if (a == null) {
            throw new RestException("TagDomain Not Found");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        try {
            a.getServices().add(s);
            return tagDomainRepository.save(a);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(e.getMessage());
        }
    }

    //Disassociate TagDomain with Service
    @RequestMapping(value = {"admin/tagDomains/{tagDomainUrn}/services"}, method = RequestMethod.DELETE)
    public final void serviceRemoveTagDomains(@RequestParam(value = "serviceUrn", required = true) String serviceUrn, @PathVariable("tagDomainUrn") String tagDomainUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/tagDomains/services/remove"
                , "tagDomainUrn", tagDomainUrn
                , "serviceUrn", serviceUrn
        );

        LOGGER.info("DELETE serviceRemoveTagDomains");

        Service s = serviceRepository.findByUrn(serviceUrn);
        if (s == null) {
            throw new RestException("Service Not Found");
        }
        TagDomain a = tagDomainRepository.findByUrn(tagDomainUrn);
        if (a == null) {
            throw new RestException("TagDomain Not Found");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        try {
            a.getServices().remove(s);
            tagDomainRepository.save(a);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(e.getMessage());
        }
    }


    // Services METHODS-----------------------------------------------

    //Create Service
    @RequestMapping(value = {"admin/services"}, method = RequestMethod.POST)
    public final Service servicesCreate(@RequestBody Service service, Principal principal) {
        kpiService.addEvent(principal, "api:admin/services/create", "serviceUrn", service.getUrn());

        LOGGER.info("POST servicesCreate");

        if (service.getId() != null) {
            throw new RestException("Service Exception: Service.id has to be null");
        }
        Service a = serviceRepository.findByUrn(service.getUrn());
        if (a != null) { //tagDomain Create
            throw new RestException("Service Exception: duplicate urn");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        service.setId(null);
        try {
            service = serviceRepository.save(service);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(e.getMessage());
        }
        return service;
    }

    //Delete Service
    @RequestMapping(value = {"admin/services/{serviceUrn}"}, method = RequestMethod.DELETE)
    public final void serviceDelete(@PathVariable("serviceUrn") String serviceUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/services/delete", "serviceUrn", serviceUrn);

        LOGGER.info("DELETE serviceDelete");

        Service s = serviceRepository.findByUrn(serviceUrn);
        if (s == null) {
            throw new RestException("Service Not Found");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        try {
            serviceRepository.delete(s);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(e.getMessage());
        }
    }


    // Experiment METHODS-----------------------------------------------

    //Create Experiment
    @RequestMapping(value = {"admin/experiments"}, method = RequestMethod.POST)
    public final ExperimentDTO experimentsCreate(@RequestBody ExperimentDTO experimentDTO
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/experiments/create", "experimentUrn", experimentDTO.getUrn());

        LOGGER.info("POST experimentsCreate");

        if (experimentDTO.getId() != null) {
            throw new RestException("Experiment Exception: Experiment.id has to be null");
        }
        Application a = applicationRepository.findByUrn(experimentDTO.getUrn());
        if (a != null) { //tagDomain Create
            throw new RestException("Experiment Exception: duplicate urn");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        Application experiment = new Application();
        experiment.setId(null);
        experiment.setUrn(experimentDTO.getUrn());
        experiment.setDescription(experimentDTO.getDescription());
        try {
            experiment = applicationRepository.save(experiment);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(e.getMessage());
        }
        return dtoService.toDTO(experiment);
    }


    //Delete Application
    @RequestMapping(value = {"admin/experiments/{experimentUrn}"}, method = RequestMethod.DELETE)
    public final void experimentDelete(@PathVariable("experimentUrn") String experimentUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/experiments/delete", "experimentUrn", experimentUrn);

        LOGGER.info("DELETE experimentDelete");

        Application a = applicationRepository.findByUrn(experimentUrn);
        if (a == null) {
            throw new RestException("Experiment Not Found");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        if (ou.ownsExperiment(experimentUrn)) {
            throw new RestException("Experimenter is not owning experiment");
        }
        //todo extra checks (e.g. existing taggings etc)
        try {
            applicationRepository.delete(a);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestException(e.getMessage());
        }
    }

    //Show all available tag domains for application/experiment
    @RequestMapping(value = {"admin/experiments/{experimentUrn}/tagDomains"}, method = RequestMethod.GET) //todo
    public final List<TagDomain> applicationGetTagDomains(@PathVariable("experimentUrn") String experimentUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/experiments/tagDomains", "experimentUrn", experimentUrn);

        LOGGER.info("GET experimentGetTagDomains");

        Application a = applicationRepository.findByUrn(experimentUrn);
        if (a == null) {
            throw new RestException("Experiment Not Found");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        if (ou.ownsExperiment(experimentUrn)) {
            throw new RestException("Experimenter is not owning experiment");
        }
        return a.getTagDomains();
    }


    @RequestMapping(value = {"admin/experiments/{experimentUrn}/tagDomains"}, method = RequestMethod.POST)
    public final ExperimentDTO experimentAddTagDomains(@RequestParam(value = "tagDomainUrn", required = true) List<String> tagDomainUrns, @PathVariable("experimentUrn") String experimentUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/experiments/tagDomains/add"
                , "experimentUrn", experimentUrn
                , "tagDomainUrns", tagDomainUrns
        );

        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }

        LOGGER.info("POST experimentAddTagDomains");

        for (String tagDomainUrn : tagDomainUrns) {

            Application a = applicationRepository.findByUrn(experimentUrn);
            if (a == null) {
                throw new RestException("Experiment Not Found");
            }
            TagDomain td = tagDomainRepository.findByUrn(tagDomainUrn);
            if (td == null) {
                throw new RestException("TagDomain Not Found");
            }

            if (ou.ownsExperiment(experimentUrn)) {
                throw new RestException("Experimenter is not owning experiment");
            }

            try {
                a.getTagDomains().add(td);
                a = applicationRepository.save(a);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RestException(e.getMessage());
            }
        }

        return dtoService.toDTO(applicationRepository.findByUrn(experimentUrn));

    }

    @RequestMapping(value = {"admin/experiments/{experimentUrn}/tagDomains"}, method = RequestMethod.DELETE)
    public final void experimentRemoveTagDomains(@RequestParam(value = "tagDomainUrn", required = true) List<String> tagDomainUrns, @PathVariable("experimentUrn") String experimentUrn
            , Principal principal) {
        kpiService.addEvent(principal, "api:admin/experiments/tagDomains/remove"
                , "experimentUrn", experimentUrn
                , "tagDomainUrns", tagDomainUrns
        );

        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }

        LOGGER.info("DELETE experimentRemoveTagDomains");

        for (String tagDomainUrn : tagDomainUrns) {

            Application a = applicationRepository.findByUrn(experimentUrn);
            if (a == null) {
                throw new RestException("Experiment Not Found");
            }
            TagDomain td = tagDomainRepository.findByUrn(tagDomainUrn);
            if (td == null) {
                throw new RestException("TagDomain Not Found");
            }

            if (ou.ownsExperiment(experimentUrn)) {
                throw new RestException("Experimenter is not owning experiment");
            }
            try {
                a.getTagDomains().remove(td);
                applicationRepository.save(a);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RestException(e.getMessage());
            }
        }
    }

    //Add tag to domain
    private Tag addTag2Domain(String tagDomainUrn, TagDTO tag) {
        TagDomain d = tagDomainRepository.findByUrn(tagDomainUrn);
        if (d == null) {
            LOGGER.error("TagDomain Not Found");
            throw new RestException("TagDomain Not Found");
        }
        Tag a = tagRepository.findByUrn(tag.getUrn());
        if (a != null) {
            LOGGER.error("Tag:Duplicate Urn");
            throw new RestException("Tag:Duplicate Urn");
        }
        OrganicityAccount ou = OrganicityUserDetailsService.getCurrentUser();
        if (!ou.isAdministrator() && !ou.isExperimenter()) {
            LOGGER.error("Not Authorized Access");
            throw new RestException("Not Authorized Access");
        }
        if (ou.isTheOnlyExperimnterUsingTagDomain(applicationRepository.findApplicationsUsingTagDomain(tagDomainUrn))) {
            LOGGER.error("TagDomain is used also from other experiments. Not possible to delete/update");
            throw new RestException("TagDomain is used also from other experiments. Not possible to delete/update");
        }

        a.setId(null);
        try {
            a.setUrn(tag.getUrn());
            a.setName(tag.getName());
            a = tagRepository.save(a);
            d.getTags().add(a);
            tagDomainRepository.save(d);
            return a;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RestException(e.getMessage());
        }
    }
}
