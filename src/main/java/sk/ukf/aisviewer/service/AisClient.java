package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.AppLog;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.ScheduleEntry;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.ais.AisSession;
import sk.ukf.aisviewer.service.ais.ExamsPage;
import sk.ukf.aisviewer.service.ais.LoginPage;
import sk.ukf.aisviewer.service.ais.SchedulePage;
import sk.ukf.aisviewer.service.ais.SubjectsPage;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Vstupný bod pre prácu s AIS2. Samotné čítanie stránok je v balíku {@code service.ais}:
 * <ul>
 *   <li>{@link AisSession} – spustenie a zatvorenie prehliadača</li>
 *   <li>{@link LoginPage} – prihlásenie a zápisné listy</li>
 *   <li>{@link SubjectsPage}, {@link ExamsPage}, {@link SchedulePage} – predmety, skúšky, rozvrh</li>
 * </ul>
 */
public class AisClient {

    private static final Logger LOG = AppLog.get(AisClient.class);

    private AisSession session;
    private SubjectsPage subjectsPage;
    private ExamsPage examsPage;
    private SchedulePage schedulePage;
    private StudentInfo currentStudent;

    /**
     * Prihlási sa do AIS2 a zistí zápisné listy študenta.
     *
     * @return false pri nesprávnom mene alebo hesle
     */
    public boolean login(String username, String password) throws Exception {
        session = AisSession.open();
        try {
            LoginPage loginPage = new LoginPage(session);
            LOG.info("Prihlasovanie...");
            if (!loginPage.submit(username, password)) {
                LOG.info("Login neúspešný – nesprávne prihlasovacie údaje.");
                closeSession();
                return false;
            }

            LOG.info("Prihlásený, načítavam údaje...");
            currentStudent = loginPage.openStudentHome(username);

            // Zápisné listy z cache sa použijú ako základ, AIS ich potom doplní o nové.
            LocalCacheService cacheService = new LocalCacheService();
            LocalCacheService.CacheSnapshot cachedSnapshot = cacheService.load().orElse(null);
            boolean cacheLoaded = isCacheForCurrentStudent(cachedSnapshot);
            if (cacheLoaded) {
                StudentInfo cachedStudent = cachedSnapshot.getStudentInfo();
                currentStudent.setEnrollmentListId(cachedStudent.getEnrollmentListId());
                currentStudent.setEnrollmentListIds(new ArrayList<>(cachedStudent.getEnrollmentListIds()));
                currentStudent.setEnrollmentListNames(new ArrayList<>(cachedStudent.getEnrollmentListNames()));
                LOG.info("Platná cache nájdená, zápisné listy sa overia v AiS2.");
            }

            LOG.info("Hľadám všetky zápisné listy v AiS2..." + (cacheLoaded ? " (cache je iba základ)" : ""));
            loginPage.discoverEnrollments(currentStudent);
            logEnrollments();

            if (cachedSnapshot != null) {
                cachedSnapshot.setStudentInfo(currentStudent);
                try {
                    cacheService.save(cachedSnapshot);
                    LOG.fine("Aktualizované údaje o zápisných listoch uložené do cache.");
                } catch (Exception e) {
                    LOG.warning("Údaje o zápisných listoch sa nepodarilo uložiť: " + e.getMessage());
                }
            }

            subjectsPage = new SubjectsPage(session);
            examsPage = new ExamsPage(session);
            schedulePage = new SchedulePage(session);
            return true;
        } catch (Exception e) {
            LOG.warning("Prihlásenie zlyhalo: " + e.getMessage());
            closeSession();
            throw e;
        }
    }

    public List<Subject> fetchSubjects(String enrollmentListId) {
        return isReady() ? subjectsPage.fetchSubjects(enrollmentListId) : new ArrayList<>();
    }

    public List<Exam> fetchExams(String enrollmentListId) {
        return isReady() ? examsPage.fetchExams(enrollmentListId) : new ArrayList<>();
    }

    public List<ScheduleEntry> fetchScheduleEntries(String enrollmentListId) {
        return isReady() ? schedulePage.fetchScheduleEntries(enrollmentListId) : new ArrayList<>();
    }

    public StudentInfo getCurrentStudent() {
        return currentStudent;
    }

    public void logout() {
        closeSession();
        currentStudent = null;
    }

    private boolean isReady() {
        return session != null && session.isOpen() && subjectsPage != null;
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
        subjectsPage = null;
        examsPage = null;
        schedulePage = null;
    }

    private void logEnrollments() {
        List<String> ids = currentStudent.getEnrollmentListIds();
        List<String> names = currentStudent.getEnrollmentListNames();
        for (int i = 0; i < ids.size(); i++) {
            LOG.info("Zápisný list: " + (i < names.size() ? names.get(i) : "?") + ", id=" + ids.get(i)
                    + (ids.get(i).equals(currentStudent.getEnrollmentListId()) ? " (aktuálny)" : ""));
        }
    }

    private boolean isCacheForCurrentStudent(LocalCacheService.CacheSnapshot snapshot) {
        if (snapshot == null || snapshot.getStudentInfo() == null
                || currentStudent == null) {
            return false;
        }

        StudentInfo cachedStudent = snapshot.getStudentInfo();
        String cachedName = cachedStudent.getFullName();
        String currentName = currentStudent.getFullName();
        return cachedName != null && currentName != null
                && cachedName.equalsIgnoreCase(currentName)
                && cachedStudent.getEnrollmentListIds() != null
                && cachedStudent.getEnrollmentListNames() != null
                && !cachedStudent.getEnrollmentListIds().isEmpty()
                && cachedStudent.getEnrollmentListIds().size()
                        == cachedStudent.getEnrollmentListNames().size()
                && snapshot.getEnrollmentData() != null
                && !snapshot.getEnrollmentData().isEmpty();
    }
}
