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
    void subjectDetailsHaveNoImportArtifacts() {
        var catalogs = new StudyPlanCatalogService();
        var subjects = new java.util.ArrayList<>(catalogs.load(DegreeLevel.BACHELOR, StudyForm.FULL_TIME)
                .orElseThrow().getSubjects());
        subjects.addAll(catalogs.load(DegreeLevel.MASTER, StudyForm.FULL_TIME).orElseThrow().getSubjects());
        for (var subject : subjects) {
            SubjectInfo info = service.findByCode(subject.getCode()).orElseThrow();
            assertFalse(info.getLanguage().toLowerCase().startsWith("je potrebná"),
                    "jazyk obsahuje zvyšok nadpisu: " + info.getCode());
            assertTrue(info.getTeachers().stream().noneMatch(t -> t.matches("(?i)ph\\.?d\\.?|csc\\.?")),
                    "titul ako samostatný vyučujúci: " + info.getCode());
        }
    }

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
    void mathSubjectsFromOtherDepartmentAreInCatalogAndCountAsCompleted() {
        var bachelor = new StudyPlanCatalogService()
                .load(DegreeLevel.BACHELOR, StudyForm.FULL_TIME).orElseThrow();
        var mpi1 = bachelor.getSubjects().stream()
                .filter(s -> s.getCode().equals("KM/MPI1/22")).findFirst().orElseThrow();
        assertEquals(3, mpi1.getCredits());
        assertEquals(sk.ukf.aisviewer.model.SubjectType.COMPULSORY_ELECTIVE, mpi1.getType());
        assertEquals("Matematika pre informatikov 1", service.findByCode("KM/MPI1/22").orElseThrow().getName());

        // Študent má v AIS kód len "MPI1" – musí sa spárovať s KM/MPI1/22.
        var ais = new sk.ukf.aisviewer.model.Subject("Matematika pre informatikov 1", "MPI1", "3K",
                "ZS", "A", "ABS - absolvoval/a", "", "Povinne voliteľné predmety");
        assertTrue(CompletedSubjectsService.completedCodes(bachelor, java.util.List.of(ais)).contains("mpi1"));
    }

    @Test
    void everyCurrentCatalogSubjectHasItsAvailableDetail() {
        StudyPlanCatalogService catalogs = new StudyPlanCatalogService();
        var bachelor = catalogs.load(DegreeLevel.BACHELOR, StudyForm.FULL_TIME).orElseThrow();
        var master = catalogs.load(DegreeLevel.MASTER, StudyForm.FULL_TIME).orElseThrow();
        // 55 predmetov KI + 2 z Katedry matematiky (KM/MPI1/22, KM/MPI2/22)
        assertEquals(57, bachelor.getSubjects().size());
        assertEquals(31, master.getSubjects().size());
        assertTrue(bachelor.getSubjects().stream().allMatch(s -> service.findByCode(s.getCode()).isPresent()));
        assertTrue(master.getSubjects().stream().allMatch(s -> service.findByCode(s.getCode()).isPresent()));
    }
}
