import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

class Board {
    private static final Random rng = new Random();

    private boolean[][] mines;
    private boolean[][] clicked;
    private boolean[][] flagged;
    private int[][] adjacent;
    private boolean firstClick;
    private long startTime;
    protected int width, height, numMines;

    // must call reset once before use
    public Board(Difficulty difficulty) {
        changeDifficulty(difficulty);
    }

    protected boolean isMine(int row, int col) {
        return mines[row][col];
    }

    protected boolean clicked(int row, int col) {
        return clicked[row][col];
    }

    protected boolean setClicked(int row, int col) {
        if (flagged(row, col))
            return false;
        clicked[row][col] = true;
        if (firstClick) {
            boolean good = false;
            while (!good) {
                good = true;
                for (int r = Math.max(0, row - 1); r <= Math.min(height - 1, row + 1); r++) {
                    for (int c = Math.max(0, col - 1); c <= Math.min(width - 1, col + 1); c++) {
                        moveMine(r, c);
                    }
                }
                for (int r = Math.max(0, row - 1); r <= Math.min(height - 1, row + 1); r++) {
                    for (int c = Math.max(0, col - 1); c <= Math.min(width - 1, col + 1); c++) {
                        if (mines[r][c]) {
                            good = false;
                            break;
                        }
                    }
                    if (!good)
                        break;
                }
            }
            startTime = System.currentTimeMillis();
        }
        firstClick = false;
        return true;
    }

    protected boolean flagged(int row, int col) {
        return flagged[row][col];
    }

    protected void setFlagged(int row, int col) {
        if (clicked(row, col))
            return;
        flagged[row][col] = !flagged[row][col];
    }

    protected int adjacentMines(int row, int col) {
        return adjacent[row][col];
    }

    private void moveMine(int row, int col) {
        if (mines[row][col]) {
            int r, c;
            do {
                r = rng.nextInt(height);
                c = rng.nextInt(width);
            }
            while (mines[r][c] || r != row && c != col);
            mines[row][col] = false;
            mines[r][c] = true;
            updateAdjacent(row, col, -1);
            updateAdjacent(r, c, 1);
        }
    }

    protected void showMines() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                setClicked(i, j);
            }
        }
    }

    private void updateAdjacent(int row, int col, int increment) {
        for (int r = Math.max(0, row - 1); r <= Math.min(height - 1, row + 1); r++) {
            for (int c = Math.max(0, col - 1); c <= Math.min(width - 1, col + 1); c++) {
                adjacent[r][c] += increment;
            }
        }
    }

    protected void reset() {
        this.mines = new boolean[height][width];
        this.clicked = new boolean[height][width];
        this.flagged = new boolean[height][width];
        this.adjacent = new int[height][width];
        this.firstClick = true;
        this.startTime = 0;
        for (int i = 0; i < numMines; i++) {
            int r, c;
            do {
                r = rng.nextInt(height);
                c = rng.nextInt(width);
            }
            while (mines[r][c]);
            mines[r][c] = true;
            updateAdjacent(r, c, 1);
        }
    }

    protected void changeDifficulty(Difficulty newDifficulty) {
        int rows = newDifficulty.rows, cols = newDifficulty.cols, numMines = newDifficulty.mines;
        assert numMines <= rows * cols;
        this.numMines = numMines;
        this.width = cols;
        this.height = rows;
    }

    public boolean hasNumber(int n) {
        assert n >= 0 && n <= 8;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (adjacent[r][c] == n) {
                    return true;
                }
            }
        }
        return false;
    }

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public boolean isGameActive() {
        return !firstClick;
    }

    /**
     * Return the current state of the tile of the given coordinates
     * @param row the row of the tile
     * @param col the column of the tile
     * @return 0+ for adjacent mines, -1 for undiscovered tiles
     */
    public int getState(int row, int col) {
        if (clicked(row, col))
            return adjacentMines(row, col);
        return -1;
    }
}

public class BoardFrame extends Board implements ActionListener, MouseListener {
    private static final ImageIcon NORMAL, WIN, LOSE;
    private static final Color[] NUMBER_COLORS = new Color[] { Color.BLACK, Color.BLUE, new Color(75, 166, 67), Color.RED, new Color(116, 25, 169), new Color(255, 120, 0), Color.CYAN };
    private static final Font font = new Font("Source Sans", Font.BOLD, 24);
    private JButton[][] buttons;
    private final JFrame frame;
    private final JPanel buttonPanel;
    private final GridLayout layout;
    private final JButton resetButton;
    private final JLabel timer;
    private boolean gameOver, win;
    private int numClicked;

    private Solver solver;

    static {
        String[] fileNames = new String[] { "nerd.png", "grinning.png", "dizzy.png" };
        ImageIcon[] icons = new ImageIcon[fileNames.length];
        for (int i = 0; i < fileNames.length; i++)
            icons[i] = new ImageIcon(new ImageIcon(fileNames[i]).getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH));
        NORMAL = icons[0];
        WIN = icons[1];
        LOSE = icons[2];
    }

    public BoardFrame() {
        this(Difficulty.EASY);
    }

    public BoardFrame(Difficulty difficulty) {
        super(difficulty);

        frame = new JFrame();
        frame.setResizable(false);
        frame.setTitle("Minesweeper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);
        ButtonGroup difficultyGroup = new ButtonGroup();
        Difficulty[] difficulties = Difficulty.values();
        for (int i = 0; i < difficulties.length; i++) {
            Difficulty currentDifficulty = difficulties[i];
            JRadioButtonMenuItem menuButton = new JRadioButtonMenuItem(currentDifficulty.name);
            if (i == 0) {
                menuButton.setSelected(true);
            }
            menuButton.setActionCommand(currentDifficulty.name);
            menuButton.addActionListener(e -> {
                if (e.getActionCommand().equals(menuButton.getActionCommand())) {
                    changeDifficulty(currentDifficulty);
                    recreateButtons();
                    reset();
                }
            });
            difficultyGroup.add(menuButton);
            gameMenu.add(menuButton);
        }
        menuBar.add(gameMenu);
        solver = new Solver(this);
        JMenu solverMenu = new JMenu("Solver");
        solverMenu.setMnemonic(KeyEvent.VK_J);
        JMenuItem solverSimple = new JMenuItem("Simple");
        solverSimple.setActionCommand("Solver Simple");
        solverSimple.addActionListener(e -> {
            if (e.getActionCommand().equals("Solver Simple")) {
                solver.solveSimple();
            }
        });
        solverMenu.add(solverSimple);
        JMenuItem solverMain = new JMenuItem("Main");
        solverMain.setActionCommand("Solver Main");
        solverMain.addActionListener(e -> {
            if (e.getActionCommand().equals("Solver Main")) {
                solver.solve();
            }
        });
        solverMenu.add(solverMain);
        menuBar.add(solverMenu);
        frame.setJMenuBar(menuBar);

        Container contentPane = frame.getContentPane();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(80, 100));
        panel.setSize(new Dimension(80, 100));
        resetButton = new JButton();
        resetButton.setIcon(NORMAL);
        resetButton.setBackground(Color.WHITE);
        resetButton.setSelectedIcon(null);
        resetButton.setRolloverIcon(null);
        resetButton.setPressedIcon(null);
        resetButton.addMouseListener(this);
        resetButton.setPreferredSize(new Dimension(80, 80));
        resetButton.setSize(80, 80);
        resetButton.setAlignmentX(.5f);
        resetButton.setMaximumSize(new Dimension(80, 80));
        panel.add(resetButton);
        timer = new JLabel();
        timer.setFont(font);
        timer.setPreferredSize(new Dimension(80, 20));
        timer.setSize(80, 20);
        timer.setText(" ");
        timer.setHorizontalAlignment(SwingConstants.CENTER);
        timer.setAlignmentX(.5f);
        panel.add(timer);
        panel.setPreferredSize(new Dimension(frame.getWidth(), 100));
        panel.setSize(new Dimension(frame.getWidth(), 100));
        contentPane.add(panel, "North");

        layout = new GridLayout(height, width, 0, 0);
        buttonPanel = new JPanel(layout);

        contentPane.add(buttonPanel);
        recreateButtons();

        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTimer();
            }
        }, 0, 100);

        reset();

        frame.setVisible(true);
        frame.toFront();
    }

    private void updateTimer() {
        if (gameOver)
            return;

        if (isGameActive())
            timer.setText(getElapsedSeconds() + "");
        else
            timer.setText(" ");
    }

    private void updateButton(int row, int col) {
        JButton button = buttons[row][col];
        if (clicked(row, col)) {
            if (isMine(row, col)) {
                button.setText("💣");
                button.setForeground(Color.BLACK);
                if (!win)
                    onLose();
            }
            else {
                int numAdjacent = adjacentMines(row, col);
                button.setText(numAdjacent == 0 ? "" : numAdjacent + "");
                button.setBackground(Color.LIGHT_GRAY);
                if (numAdjacent > NUMBER_COLORS.length)
                    button.setForeground(Color.BLACK);
                else
                    button.setForeground(NUMBER_COLORS[numAdjacent]);
            }
        }
        else {
            button.setBackground(Color.WHITE);
            button.setText("");
            if (flagged(row, col)) {
                button.setText("🚩");
                button.setForeground(Color.RED);
            }
        }
    }

    private void onGameOver() {
        gameOver = true;
    }

    private void onWin() {
        onGameOver();
        win = true;
        resetButton.setIcon(WIN);
        showMines();
        forceUpdateButtons();
    }

    private void onLose() {
        onGameOver();
        resetButton.setIcon(LOSE);
    }

    public void doClick(int row, int col) {
        if (clicked(row, col) || gameOver)
            return;

        boolean success = setClicked(row, col);
        if (!success)
            return;
        updateButton(row, col);
        numClicked++;
        if (numClicked == width * height - numMines) {
            onWin();
        }
        if (adjacentMines(row, col) == 0) {
            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (r >= 0 && r < height && c >= 0 && c < width) {
                        doClick(r, c);
                    }
                }
            }
        }
    }

    public void doFlag(int row, int col) {
        if (clicked(row, col) || gameOver)
            return;

        setFlagged(row, col);
        updateButton(row, col);
    }

    private void forceUpdateButtons() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                updateButton(i, j);
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        gameOver = false;
        win = false;
        numClicked = 0;
        resetButton.setIcon(NORMAL);
        forceUpdateButtons();
    }

    private void recreateButtons() {
        buttonPanel.removeAll();
        layout.setRows(height);
        layout.setColumns(width);
        frame.setSize(40 * width, 40 * height + 120);
        buttonPanel.setPreferredSize(new Dimension(40, 40));
        buttonPanel.setSize(40 * width, 40 * height);
        Dimension dimension = new Dimension(40, 40);
        buttons = new JButton[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                JButton button = new JButton();
                button.setBackground(Color.WHITE);
                button.setFont(font);
                button.setPreferredSize(dimension);
                button.setSize(dimension);
                button.setMargin(new Insets(0, 0, 0, 0));
                button.setSelectedIcon(null);
                button.setRolloverIcon(null);
                button.setPressedIcon(null);
                //button.addActionListener(this);
                button.addMouseListener(this);
                button.setActionCommand(i + " " + j);
                buttonPanel.add(button);
                buttons[i][j] = button;
            }
        }
    }

    private void handleClick(MouseEvent e) {
        if (e.getSource() instanceof JButton button) {
            if (button == resetButton) {
                reset();
            }
            else {
                String[] tokens = button.getActionCommand().split(" ");
                int row = Integer.parseInt(tokens[0]), col = Integer.parseInt(tokens[1]);
                if (e.getButton() == MouseEvent.BUTTON1)
                    doClick(row, col);
                else if (e.getButton() == MouseEvent.BUTTON3)
                    doFlag(row, col);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        /*
        if (e.getSource() instanceof JButton button) {
            if (button == resetButton) {
                reset();
            }
            else {
                String[] tokens = button.getName().split(" ");
                int row = Integer.parseInt(tokens[0]), col = Integer.parseInt(tokens[1]);
                doClick(row, col);
            }
        }
         */
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        handleClick(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
