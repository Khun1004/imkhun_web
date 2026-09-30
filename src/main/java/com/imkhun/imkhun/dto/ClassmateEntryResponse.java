package com.imkhun.imkhun.dto;

import java.util.List;

// 나의 시간표에 있는 수업 하나에 대해, 그 수업을 같이 듣는 친구들의 닉네임 목록.
// "이 수업엔 OO님도 같이 들어요" 처럼 보여주는 용도 (친구 사이인 사람만 대상)
public record ClassmateEntryResponse(String courseName, List<String> friendNicknames) {
}