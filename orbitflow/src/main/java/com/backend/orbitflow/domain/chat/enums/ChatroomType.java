package com.backend.orbitflow.domain.chat.enums;

// DIRECT : 두 사용자 사이의 1:1 대화 (참여자 추가 불가, 초대 시 새 GROUP 생성)
// GROUP : 3명 이상이 참여해 생성한 대화 (이후 2명 이하가 되어도 GROUP 유지)
public enum ChatroomType {
    DIRECT, GROUP
}
