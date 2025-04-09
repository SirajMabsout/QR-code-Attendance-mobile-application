package Capstone.QR.utils;

import Capstone.QR.model.ClassSession;
import Capstone.QR.model.Klass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GenerateSessions {

    public static List<ClassSession> generateSessionsForClass(Klass klass) {
        List<ClassSession> sessions = new ArrayList<>();
        LocalDate current = klass.getStartDate();
        while (!current.isAfter(klass.getEndDate())) {
            if (klass.getScheduledDays().contains(current.getDayOfWeek())) {
                ClassSession session = new ClassSession();
                session.setKlass(klass);
                session.setSessionDate(current);
                session.setSessionTime(klass.getClassTime());
                sessions.add(session);
            }
            current = current.plusDays(1);
        }
        return sessions;
    }

}
