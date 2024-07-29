public enum Difficulty {
    EASY(8, 10, 10, "Easy"),
    MEDIUM(16, 16, 40, "Medium"),
    HARD(16, 30, 99, "Hard");

    public final int rows, cols, mines;
    public final String name;

    Difficulty(int rows, int cols, int mines, String name) {
        this.rows = rows;
        this.cols = cols;
        this.mines = mines;
        this.name = name;
    }
}
