package com.imkhun.imkhun.service;

import com.imkhun.imkhun.domain.Application;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

// 학생의 수업 시간표를 .ics 파일로 만들어주는 서비스 — 구글/애플/아웃룩 캘린더 어디든 가져오기(import) 할 수 있음
@Service
public class CalendarExportService {

    // 요일 코드(MON~SUN) → iCalendar에서 쓰는 2글자 코드
    private static final Map<String, String> ICAL_DAY_CODE = Map.of(
            "MON", "MO", "TUE", "TU", "WED", "WE", "THU", "TH", "FRI", "FR", "SAT", "SA", "SUN", "SU"
    );

    private static final Map<String, DayOfWeek> DAY_OF_WEEK = Map.of(
            "MON", DayOfWeek.MONDAY, "TUE", DayOfWeek.TUESDAY, "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY, "FRI", DayOfWeek.FRIDAY, "SAT", DayOfWeek.SATURDAY, "SUN", DayOfWeek.SUNDAY
    );

    private static final DateTimeFormatter ICAL_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICAL_STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    // 수업 기본 길이(끝나는 시간을 안 받고 있어서, 1시간짜리로 가정함)
    private static final int DEFAULT_CLASS_DURATION_MINUTES = 60;

    public String buildIcsForApplications(List<Application> applications) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//ImKhun//KWZM Center//KO\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");

        // 시간대를 명확히 정의해줌 — 이게 없으면 일부 캘린더 앱(특히 오래된 아웃룩 등)이 TZID=Asia/Seoul을
        // 못 알아보고 일정을 아예 안 보여주는 경우가 있어서, 표준 형식으로 직접 정의해줌 (한국은 서머타임 없음, 항상 UTC+9)
        sb.append("BEGIN:VTIMEZONE\r\n");
        sb.append("TZID:Asia/Seoul\r\n");
        sb.append("BEGIN:STANDARD\r\n");
        sb.append("DTSTART:19700101T000000\r\n");
        sb.append("TZOFFSETFROM:+0900\r\n");
        sb.append("TZOFFSETTO:+0900\r\n");
        sb.append("END:STANDARD\r\n");
        sb.append("END:VTIMEZONE\r\n");

        LocalDateTime now = LocalDateTime.now();
        String stamp = now.format(ICAL_STAMP_FORMAT);

        for (Application application : applications) {
            if (application.getClassDays() == null || application.getClassTime() == null) continue;

            List<String> dayCodes = List.of(application.getClassDays().split(","));
            if (dayCodes.isEmpty()) continue;

            LocalTime classTime;
            try {
                classTime = LocalTime.parse(application.getClassTime());
            } catch (Exception e) {
                continue; // 시간 형식이 이상하면 이 강의는 건너뜀
            }

            LocalDate firstOccurrence = findNearestUpcomingDate(dayCodes, now.toLocalDate());
            LocalDateTime dtStart = LocalDateTime.of(firstOccurrence, classTime);
            LocalDateTime dtEnd = dtStart.plusMinutes(DEFAULT_CLASS_DURATION_MINUTES);

            String byDay = String.join(",", dayCodes.stream().map(ICAL_DAY_CODE::get).filter(java.util.Objects::nonNull).toList());

            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:imkhun-application-").append(application.getId()).append("@imkhun\r\n");
            sb.append("DTSTAMP:").append(stamp).append("\r\n");
            sb.append("DTSTART;TZID=Asia/Seoul:").append(dtStart.format(ICAL_DATETIME_FORMAT)).append("\r\n");
            sb.append("DTEND;TZID=Asia/Seoul:").append(dtEnd.format(ICAL_DATETIME_FORMAT)).append("\r\n");
            sb.append("RRULE:FREQ=WEEKLY;BYDAY=").append(byDay).append("\r\n");
            sb.append("SUMMARY:").append(escapeIcsText(application.getCourseName())).append("\r\n");
            sb.append("DESCRIPTION:").append(escapeIcsText("I'm Khun / KWZM Center 수업")).append("\r\n");
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    // 오늘(포함)부터 시작해서, 요일 목록 중 가장 가까운 날짜를 찾음
    private LocalDate findNearestUpcomingDate(List<String> dayCodes, LocalDate from) {
        for (int i = 0; i < 7; i++) {
            LocalDate candidate = from.plusDays(i);
            DayOfWeek candidateDay = candidate.getDayOfWeek();
            for (String dayCode : dayCodes) {
                if (candidateDay == DAY_OF_WEEK.get(dayCode)) {
                    return candidate;
                }
            }
        }
        return from; // 이론상 여기 안 옴 (dayCodes가 비어있지 않은 이상)
    }

    // 쉼표, 세미콜론, 줄바꿈 등은 iCalendar 규격상 이스케이프 필요
    private String escapeIcsText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}