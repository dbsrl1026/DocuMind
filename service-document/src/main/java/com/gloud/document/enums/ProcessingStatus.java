package com.gloud.document.enums;

public enum ProcessingStatus {
    PENDING,     // 대기 (Kafka 전송 전 or 수신 대기 상태)
    PROCESSING,  // AI 처리 중 (ai-service에서 작업 수행 중)
    COMPLETED,   // 벡터 DB 저장 완료
    FAILED       // 실패 (AI 분석 오류 등)
}
