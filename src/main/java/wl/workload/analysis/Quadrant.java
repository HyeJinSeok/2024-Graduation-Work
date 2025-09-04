package wl.workload.analysis;

//PCA (u,v) 좌표에 따른 사분면 구분 + 톤 메시지 제공
public enum Quadrant {
    Q1("속도·집중을 높이는 방향 추천"),
    Q2("목표·범위·품질을 다듬는 방향 추천"),
    Q3("현재 컨디션을 유지하되, 필요에 따라 몰입을 위한 동기부여 방향을 추천"),
    Q4("작업 리듬과 환경을 정돈하는 방향 추천");

    private final String tone;

    Quadrant(String tone) { this.tone = tone; }

    public String tone() { return tone; }

    // (u,v) 값으로 사분면 분류
    public static Quadrant classify(double u, double v) {
        if (u >= 0 && v >= 0) return Q1;
        if (u < 0 && v >= 0)  return Q2;
        if (u < 0 && v < 0)   return Q3;
        return Q4; // u>=0 && v<0
    }
}
