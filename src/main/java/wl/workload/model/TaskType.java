package wl.workload.model;

/*
 * 사용자가 선택할 작업 유형 5가지 (카드 UI에서 선택)
 */


//내부 코드에서 사용되는 식별자
public enum TaskType {
    MEETING("Meeting"),
    STUDY("Study"),
    CREATIVE("Creative"),
    OPS("Ops"),
    ADMIN("Admin");

    // UI 표시용 문자열
    private final String label;

    TaskType(String label) { this.label = label; }

    public String label() { return label; }
}
