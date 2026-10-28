package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.SubjectInfo;
import sk.ukf.aisviewer.model.DegreeLevel;
import sk.ukf.aisviewer.model.StudyForm;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SubjectInfoServiceTest {
    private final SubjectInfoService service = new SubjectInfoService();

    @Test
    void findsByFullCurriculumCode() {
        assertEquals("KI/PJC/26", service.findByCode("KI/PJC/26").orElseThrow().getCode());
    }

    @Test
    void missingDetailReturnsEmpty() {
        assertTrue(service.findByCode("PJC").isEmpty());
        assertTrue(service.findByCode("KI/DOES_NOT_EXIST/99").isEmpty());
    }

    @Test
    void currentVersionDoesNotUseOlderInfoSheet() {
        SubjectInfo current = service.findByCode("KI/PJC/26").orElseThrow();
        assertEquals("KI/PJC/26", current.getCode());
        assertTrue(service.findByCode("KI/PJC/22").isEmpty());
    }

    @Test
    void generatedDatasetsHaveExpectedCountsAndUniqueCodes() {
        Set<String> bachelorCodes = new HashSet<>();
        Set<String> masterCodes = new HashSet<>();
        for (String code : new String[]{"KI/AA/22", "KI/APR/26", "KI/PJC/26"}) {
            bachelorCodes.add(service.findByCode(code).orElseThrow().getCode());
        }
        assertEquals(3, bachelorCodes.size());
        assertEquals("KI/mOP/22", service.findByCode("KI/mOP/22").orElseThrow().getCode());
        assertTrue(masterCodes.add(service.findByCode("KI/mOP/22").orElseThrow().getCode()));
    }

    @Test
    void everyCurrentCatalogSubjectHasItsAvailableDetail() {
        StudyPlanCatalogService catalogs = new StudyPlanCatalogService();
        var bachelor = catalogs.load(DegreeLevel.BACHELOR, StudyForm.FULL_TIME).orElseThrow();
        var master = catalogs.load(DegreeLevel.MASTER, StudyForm.FULL_TIME).orElseThrow();
        assertEquals(55, bachelor.getSubjects().size());
        assertEquals(31, master.getSubjects().size());
        assertTrue(bachelor.getSubjects().stream().allMatch(s -> service.findByCode(s.getCode()).isPresent()));
        assertTrue(master.getSubjects().stream().allMatch(s -> service.findByCode(s.getCode()).isPresent()));
    }
}
