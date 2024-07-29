import java.util.*;

class Node {
    public final Coordinate coord;
    public int numAdjacent;
    private final List<Node> connections;
    // if there is a connecting node. the corresponding tile must be a number
    private boolean activated;

    public Node(Coordinate coord, List<Node> connections) {
        this.coord = coord;
        this.connections = connections;
    }

    public Node(Coordinate coord) {
        this(coord, new LinkedList<>());
    }

    public Node(int row, int col) {
        this(new Coordinate(row, col));
    }

    public int count() {
        return connections.size();
    }

    public void add(Node node) {
        connections.add(node);
    }

    public void remove(Node node) {
        boolean success = connections.remove(node);
        if (success) {
            numAdjacent--;
            System.out.println(this + " is removing " + node);
        }
    }

    public boolean contains(Node node) {
        return connections.contains(node);
    }

    public void breakConnections() {
        for (Node node : connections) {
            node.remove(this);
        }
        connections.clear();
    }

    public Iterable<Node> getConnections() {
        return connections;
    }

    public void activate(int adjacent) {
        activated = true;
        numAdjacent = adjacent;
        System.out.println(this + " activated");
    }

    public boolean isActivated() {
        return activated;
    }

    @Override
    public String toString() {
        return coord.row() + ", " + coord.col() + " (size=" + count() + ", adjacent=" + numAdjacent + ")";
    }
}

public class Solver {
    public BoardFrame board;

    private static final int MAX_MOVES = Integer.MAX_VALUE;

    private static final int MAX_LOOPS = 10;

    public Solver(BoardFrame board) {
        this.board = board;
    }


    // GOOD SOLVER
    
    // Each tile is a node connecting to adjacent empty tiles
    public void solve() {
        SolverInstance solver = new SolverInstance();
        solver.solve();
    }
    
    // GOOD SOLVER CLASS
    private class SolverInstance {
        private final Node[][] graph;
        private final PriorityQueue<Node> toCheck;

        public SolverInstance() {
            graph = new Node[board.height][board.width];
            for (int i = 0; i < graph.length; i++) {
                for (int j = 0; j < graph[i].length; j++) {
                    graph[i][j] = new Node(i, j);
                }
            }
            toCheck = new PriorityQueue<>((a, b) -> a.numAdjacent == 0 ? Integer.MIN_VALUE : b.numAdjacent == 0 ? Integer.MAX_VALUE : a.count() - b.count());
            if (!board.isGameActive())
                board.doClick(board.height / 2, board.width / 2);
            init();
        }

        private void init() {
            // loop through all locations
            for (int row = 0; row < board.height; row++) {
                for (int col = 0; col < board.width; col++) {
                    int state = board.getState(row, col);
                    if (state <= 0 || (state - getFlaggedAdjacent(row, col) == 0 && getEmptyNoFlagAdjacent(row, col) == 0)) {
                        continue;
                    }
                    // activate nodes with numbers (initialize numAdjacent)
                    Node node = graph[row][col];
                    // add this node to the queue
                    addNewNode(node);
                }
            }
        }

        // basically initialize nodes when they are activated()
        private void addEmptyToNode(Node node) {
            int row = node.coord.row(), col = node.coord.col();
            for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
                for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                    // add empty tiles to its connections list
                    if (board.getState(r, c) == -1 && !board.flagged(r, c))
                        node.add(graph[r][c]);
                }
            }
        }

        private void addNewNode(Node node) {
            int row = node.coord.row(), col = node.coord.col();
            addEmptyToNode(node);
            node.activate(board.getState(row, col) - getFlaggedAdjacent(row, col));
            toCheck.offer(node);
        }

        public void solve() {
            // TODO why is this thing not insta solving?
            System.out.println(toCheck);
            int loops = 0;
            while (!toCheck.isEmpty() && loops < MAX_LOOPS) {
                Node node = toCheck.peek();
                boolean used = false;
                System.out.println("Current Node is " + node);
                // click around if this tile is cleared (no unmarked adjacent mines)
                if (node.numAdjacent == 0) {
                    used = true;
                    System.out.println("Clicking around " + node);
                    int row = node.coord.row(), col = node.coord.col();
                    clickAdjacent(row, col);
                    for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
                        for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                            Node adjacent = graph[r][c];
                            if (!adjacent.isActivated() && board.getState(r, c) > 0) {
                                addEmptyToNode(adjacent);
                            }
                        }
                    }
                }
                else if (node.count() == node.numAdjacent) {
                    // if guaranteed location (empty tiles equals number of adjacent mines)
                    used = true;
                    // flag empty tiles
                    for (Node adjacent : node.getConnections()) {
                        int row = adjacent.coord.row(), col = adjacent.coord.col();
                        board.doFlag(row, col);
                        // update number tiles surrounding the mine
                        for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
                            for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                                Node adj = graph[r][c];
                                if (adj.isActivated()) {
                                    adj.remove(adjacent);
                                }
                                else if (board.getState(r, c) > 0) {
                                    addNewNode(adj);
                                }
                            }
                        }
                    }
                    node.breakConnections();
                }
                System.out.println("Used: " + used);
                if (used) {
                    toCheck.poll();
                    loops = 0;
                }
                else {
                    System.out.println(toCheck);
                    toCheck.offer(toCheck.poll());
                    loops++;
                }
            }
        }
    }

    // SIMPLE SOLVER
    public void solveSimple() {
        if (!board.isGameActive())
            board.doClick(board.height / 2, board.width / 2);

        boolean madeMove;
        int moves = 0;
        do {
            List<Integer> toClick = getSimpleFlag();
            madeMove = !toClick.isEmpty();
            moves++;
            for (Integer integer : toClick) {
                flagAdjacent(integer / board.width, integer % board.width);
            }
            toClick = getSimpleEmpty();
            madeMove = madeMove || !toClick.isEmpty();
            for (Integer integer : toClick) {
                clickAdjacent(integer / board.width, integer % board.width);
            }
        }
        while (madeMove && moves < MAX_MOVES);
    }

    // get all tiles that have guaranteed adjacent mines
    private List<Integer> getSimpleFlag() {
        List<Integer> list = new LinkedList<>();
        for (int row = 0; row < board.height; row++) {
            for (int col = 0; col < board.width; col++) {
                int state = board.getState(row, col);
                if (state <= 0)
                    continue;
                int flagged = getFlaggedAdjacent(row, col);
                int extra = state - getEmptyAdjacent(row, col) - flagged;
                if (extra == 0 && flagged != state) {
                    list.add(row * board.width + col);
                }
            }
        }
        return list;
    }

    private int getEmptyAdjacent(int row, int col) {
        int count = 0;
        for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
            for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                if (board.getState(r, c) == -1 && !board.flagged(r, c))
                    count++;
            }
        }
        return count;
    }

    // get all tiles that have adjacent flags that add up to number
    private List<Integer> getSimpleEmpty() {
        List<Integer> list = new LinkedList<>();
        for (int row = 0; row < board.height; row++) {
            for (int col = 0; col < board.width; col++) {
                int state = board.getState(row, col);
                if (state <= 0)
                    continue;
                int extra = getFlaggedAdjacent(row, col) - state;
                if (extra == 0 && getEmptyAdjacent(row, col) > 0) {
                    list.add(row * board.width + col);
                }
            }
        }
        return list;
    }

    private int getFlaggedAdjacent(int row, int col) {
        int count = 0;
        for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
            for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                if (board.flagged(r, c))
                    count++;
            }
        }
        return count;
    }

    private int getEmptyNoFlagAdjacent(int row, int col) {
        int count = 0;
        for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
            for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                if (board.getState(r, c) == -1 && !board.flagged(r, c))
                    count++;
            }
        }
        return count;
    }

    private void clickAdjacent(int row, int col) {
        for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
            for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                board.doClick(r, c);
            }
        }
    }

    private void flagAdjacent(int row, int col) {
        for (int r = Math.max(0, row - 1); r <= Math.min(board.height - 1, row + 1); r++) {
            for (int c = Math.max(0, col - 1); c <= Math.min(board.width - 1, col + 1); c++) {
                if (!board.flagged(r, c))
                    board.doFlag(r, c);
            }
        }
    }
}