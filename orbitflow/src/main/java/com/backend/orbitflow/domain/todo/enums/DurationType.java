package com.backend.orbitflow.domain.todo.enums;

// DAY : duration일마다 (사용자 지정)
// WEEK : duration주마다 days_of_week 요일 (1 = 매주, 2 = 격주)
// MONTH : duration개월마다 같은 날짜 (해당 월에 없는 날짜는 말일)
// MONTH_END : duration개월마다 말일
// YEAR : duration년마다 같은 날짜
public enum DurationType {
    DAY, WEEK, MONTH, MONTH_END, YEAR
}
