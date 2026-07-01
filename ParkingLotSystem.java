import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

// ============================================================
//  ENTRY POINT
// ============================================================
public class ParkingLotSystem {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            ParkingLot lot = new ParkingLot(20);
            ParkingManager manager = new ParkingManager(lot);
            new ParkingGUI(manager).showMainMenu();
        });
    }
}

// ============================================================
//  THEME  — central colour + font definitions
// ============================================================
class Theme {
    static final Color BG        = new Color(13, 17, 23);
    static final Color SURFACE   = new Color(22, 27, 34);
    static final Color SURFACE2  = new Color(30, 36, 45);
    static final Color BORDER    = new Color(48, 54, 61);
    static final Color ACCENT    = new Color(35, 134, 54);
    static final Color ACCENT2   = new Color(46, 164, 79);
    static final Color DANGER    = new Color(218, 54, 51);
    static final Color WARN      = new Color(210, 153, 34);
    static final Color TEXT      = new Color(230, 237, 243);
    static final Color TEXT_MUTED= new Color(110, 118, 129);
    static final Color HIGHLIGHT = new Color(56, 139, 253);

    static final Font TITLE  = new Font("SansSerif", Font.BOLD,  22);
    static final Font HEAD   = new Font("SansSerif", Font.BOLD,  15);
    static final Font BODY   = new Font("SansSerif", Font.PLAIN, 13);
    static final Font SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    static final Font MONO   = new Font("Monospaced", Font.PLAIN, 12);
    static final Font BTN    = new Font("SansSerif", Font.BOLD,  13);
}

// ============================================================
//  REUSABLE UI WIDGETS
// ============================================================

/** Rounded panel with a filled background */
class RoundPanel extends JPanel {
    private final int arc;
    RoundPanel(int arc) { this.arc = arc; setOpaque(false); }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
    }
}

/** Rounded gradient button */
class FancyButton extends JButton {
    private final Color base, hover;
    private boolean hovered = false;
    FancyButton(String text, Color base, Color hover) {
        super(text); this.base = base; this.hover = hover;
        setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false);
        setForeground(Color.WHITE); setFont(Theme.BTN); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(180, 42));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
            public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
        });
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = hovered ? hover : base;
        g2.setColor(c);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        super.paintComponent(g2);
        g2.dispose();
    }
}

/** Styled text field */
class DarkField extends JTextField {
    DarkField(int cols) {
        super(cols);
        setBackground(Theme.SURFACE2);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.TEXT);
        setFont(Theme.BODY);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }
}

/** Styled combo box */
class DarkCombo extends JComboBox<String> {
    DarkCombo(String[] items) {
        super(items);
        setBackground(Theme.SURFACE2);
        setForeground(Theme.TEXT);
        setFont(Theme.BODY);
        ((JLabel)getRenderer()).setOpaque(true);
        setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
    }
}

/** Badge label (coloured pill) */
class Badge extends JLabel {
    Badge(String text, Color bg) {
        super(text, CENTER);
        setFont(new Font("SansSerif", Font.BOLD, 10));
        setForeground(Color.WHITE);
        setBackground(bg);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
        setPreferredSize(new Dimension(80, 20));
    }
    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        super.paintComponent(g2);
        g2.dispose();
    }
}

// ============================================================
//  ABSTRACT VEHICLE  (Abstraction + Inheritance)
// ============================================================
abstract class Vehicle {
    protected final String plateNumber;
    protected final String owner;
    protected final String type;

    Vehicle(String plateNumber, String owner, String type) {
        this.plateNumber = plateNumber.toUpperCase().trim();
        this.owner       = owner.trim();
        this.type        = type;
    }

    public abstract double calculateFee(long durationMinutes);
    public abstract String getIcon();

    public String getPlateNumber() { return plateNumber; }
    public String getOwner()       { return owner; }
    public String getType()        { return type; }

    @Override public String toString() {
        return String.format("%s  %s | Owner: %s", getIcon(), plateNumber, owner);
    }
}

// ============================================================
//  CONCRETE VEHICLES  (Polymorphism)
// ============================================================
class Car extends Vehicle {
    private static final double FIRST_HOUR_FLAT  = 500.0;  // PKR for first 60 min
    private static final double EXTRA_RATE_PER_MIN = 10.0; // PKR per minute after 60 min

    Car(String plate, String owner) { super(plate, owner, "Car"); }

    @Override public double calculateFee(long durationMinutes) {
        if (durationMinutes <= 0) return 0;
        if (durationMinutes <= 60) return FIRST_HOUR_FLAT;
        long extraMinutes = durationMinutes - 60;
        return FIRST_HOUR_FLAT + (extraMinutes * EXTRA_RATE_PER_MIN);
    }

    @Override public String getIcon() { return "[CAR]"; }
    public static double getFirstHourRate()   { return FIRST_HOUR_FLAT; }
    public static double getExtraRatePerMin() { return EXTRA_RATE_PER_MIN; }
}

class Motorcycle extends Vehicle {
    private static final double FIRST_HOUR_FLAT    = 500.0;  // PKR for first 60 min
    private static final double EXTRA_RATE_PER_MIN = 10.0;   // PKR per minute after 60 min

    Motorcycle(String plate, String owner) { super(plate, owner, "Motorcycle"); }

    @Override public double calculateFee(long durationMinutes) {
        if (durationMinutes <= 0) return 0;
        if (durationMinutes <= 60) return FIRST_HOUR_FLAT;
        long extraMinutes = durationMinutes - 60;
        return FIRST_HOUR_FLAT + (extraMinutes * EXTRA_RATE_PER_MIN);
    }

    @Override public String getIcon() { return "[BIKE]"; }
    public static double getFirstHourRate()   { return FIRST_HOUR_FLAT; }
    public static double getExtraRatePerMin() { return EXTRA_RATE_PER_MIN; }
}

// ============================================================
//  PARKING RECORD  (Encapsulation)
// ============================================================
class ParkingRecord {
    private final Vehicle vehicle;
    private final int slotNumber;
    private final Date checkInTime;
    private Date checkOutTime;
    private String bookingId;
    private double feePaid;

    ParkingRecord(Vehicle vehicle, int slotNumber, Date checkInTime) {
        this.vehicle     = vehicle;
        this.slotNumber  = slotNumber;
        this.checkInTime = new Date(checkInTime.getTime());
        this.bookingId   = "PARK-" + (System.currentTimeMillis() % 100000);
    }

    void checkout(Date t) {
        this.checkOutTime = new Date(t.getTime());
        long mins = (t.getTime() - checkInTime.getTime()) / 60000;
        this.feePaid = vehicle.calculateFee(mins);
    }

    void setBookingId(String id) { this.bookingId = id; }

    Vehicle    getVehicle()     { return vehicle; }
    int        getSlotNumber()  { return slotNumber; }
    Date       getCheckInTime() { return new Date(checkInTime.getTime()); }
    Date       getCheckOutTime(){ return checkOutTime != null ? new Date(checkOutTime.getTime()) : null; }
    String     getBookingId()   { return bookingId; }
    double     getFeePaid()     { return feePaid; }
    boolean    isActive()       { return checkOutTime == null; }

    long getDurationMinutes() {
        long end = isActive() ? System.currentTimeMillis() : checkOutTime.getTime();
        return (end - checkInTime.getTime()) / 60000;
    }

    String toCSV() {
        SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String out = checkOutTime != null ? f.format(checkOutTime) : "ACTIVE";
        return vehicle.getPlateNumber()+","+vehicle.getType()+","+vehicle.getOwner()+","+
               slotNumber+","+f.format(checkInTime)+","+out+","+bookingId+","+feePaid;
    }
}

// ============================================================
//  PARKING SLOT  (Encapsulation)
// ============================================================
class ParkingSlot {
    private final int slotNumber;
    private Vehicle occupant;
    private Date occupancyStart;

    ParkingSlot(int n) { this.slotNumber = n; }

    boolean occupy(Vehicle v, Date start) {
        if (isOccupied()) return false;
        occupant       = v;
        occupancyStart = start != null ? new Date(start.getTime()) : new Date();
        return true;
    }
    boolean occupy(Vehicle v) { return occupy(v, new Date()); }

    Vehicle vacate() {
        Vehicle tmp = occupant;
        occupant = null; occupancyStart = null;
        return tmp;
    }

    boolean  isOccupied()     { return occupant != null; }
    int      getSlotNumber()  { return slotNumber; }
    Vehicle  getOccupant()    { return occupant; }
    Date     getOccupancyStartTime() { return occupancyStart != null ? new Date(occupancyStart.getTime()) : null; }

    long getOccupancyDurationMinutes() {
        if (!isOccupied()) return 0;
        return (System.currentTimeMillis() - occupancyStart.getTime()) / 60000;
    }
}

// ============================================================
//  PARKING LOT
// ============================================================
class ParkingLot {
    private final List<ParkingSlot> slots;
    private final int total;

    ParkingLot(int total) {
        this.total = total;
        slots = new ArrayList<>();
        for (int i = 1; i <= total; i++) slots.add(new ParkingSlot(i));
    }

    int  getTotalSlots()      { return total; }
    int  getAvailableCount()  { return (int) slots.stream().filter(s -> !s.isOccupied()).count(); }
    List<ParkingSlot> getAllSlots()      { return new ArrayList<>(slots); }
    List<ParkingSlot> getOccupiedSlots(){ List<ParkingSlot> r=new ArrayList<>(); for(ParkingSlot s:slots) if(s.isOccupied()) r.add(s); return r; }

    ParkingSlot getSlot(int n) { return (n < 1 || n > total) ? null : slots.get(n-1); }

    int firstFreeSlot() {
        return slots.stream().filter(s->!s.isOccupied()).mapToInt(ParkingSlot::getSlotNumber).findFirst().orElse(-1);
    }

    boolean parkVehicle(Vehicle v) {
        int n = firstFreeSlot();
        return n != -1 && slots.get(n-1).occupy(v);
    }

    boolean unparkVehicle(int n) {
        ParkingSlot s = getSlot(n);
        if (s == null || !s.isOccupied()) return false;
        s.vacate(); return true;
    }

    Vehicle getVehicleAt(int n) {
        ParkingSlot s = getSlot(n);
        return s == null ? null : s.getOccupant();
    }
}

// ============================================================
//  PARKING MANAGER  (Business logic + persistence)
// ============================================================
class ParkingManager {
    private final ParkingLot lot;
    private final List<ParkingRecord> log;
    private static final String FILE = "parking_records.txt";

    ParkingManager(ParkingLot lot) {
        this.lot = lot;
        this.log = new ArrayList<>();
        loadFromFile();
    }

    String checkIn(String plate, String owner, String type) {
        if (plate.isEmpty() || owner.isEmpty())
            return "ERROR: Plate and owner cannot be empty!";
        if (!type.equals("Car") && !type.equals("Motorcycle"))
            return "ERROR: Invalid vehicle type!";
        if (lot.getAvailableCount() == 0)
            return "ERROR: Parking lot is FULL!";
        for (ParkingRecord r : log)
            if (r.isActive() && r.getVehicle().getPlateNumber().equalsIgnoreCase(plate))
                return "ERROR: Vehicle " + plate + " is already parked!";

        Vehicle v = type.equals("Car") ? new Car(plate, owner) : new Motorcycle(plate, owner);
        if (!lot.parkVehicle(v)) return "ERROR: Failed to park vehicle!";

        int slot = slotOf(v);
        ParkingRecord rec = new ParkingRecord(v, slot, new Date());
        log.add(rec);
        save();
        return "SUCCESS|" + slot + "|" + rec.getBookingId();
    }

    String checkOut(int slotNum) {
        Vehicle v = lot.getVehicleAt(slotNum);
        if (v == null) return "ERROR: No vehicle at Slot " + slotNum;
        ParkingRecord rec = activeRecord(slotNum);
        if (rec == null) return "ERROR: No active record!";
        rec.checkout(new Date());
        lot.unparkVehicle(slotNum);
        save();
        long dur = rec.getDurationMinutes();
        return "SUCCESS|"+rec.getVehicle().getPlateNumber()+"|"+rec.getVehicle().getOwner()+"|"+
               rec.getVehicle().getType()+"|"+dur+"|"+String.format("%.2f", rec.getFeePaid())+"|"+rec.getBookingId();
    }

    String cancel(String bookingId) {
        for (int i = 0; i < log.size(); i++) {
            ParkingRecord r = log.get(i);
            if (r.getBookingId().equals(bookingId) && r.isActive()) {
                lot.unparkVehicle(r.getSlotNumber());
                log.remove(i);
                save();
                return "SUCCESS: Booking " + bookingId + " cancelled.";
            }
        }
        return "ERROR: Booking not found or already checked out!";
    }

    List<ParkingRecord> activeRecords() {
        List<ParkingRecord> r = new ArrayList<>();
        for (ParkingRecord p : log) if (p.isActive()) r.add(p);
        return r;
    }
    List<ParkingRecord> allRecords() { return new ArrayList<>(log); }
    ParkingLot getParkingLot() { return lot; }

    ParkingRecord activeRecord(int slot) {
        for (ParkingRecord r : log) if (r.getSlotNumber()==slot && r.isActive()) return r;
        return null;
    }

    private int slotOf(Vehicle v) {
        for (ParkingSlot s : lot.getAllSlots())
            if (s.isOccupied() && s.getOccupant().getPlateNumber().equals(v.getPlateNumber()))
                return s.getSlotNumber();
        return -1;
    }

    private void save() {
        try (FileWriter fw = new FileWriter(FILE)) {
            for (ParkingRecord r : log) fw.write(r.toCSV() + "\n");
        } catch (IOException e) { System.err.println("Save error: " + e.getMessage()); }
    }

    private void loadFromFile() {
        File f = new File(FILE);
        if (!f.exists()) return;
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNextLine()) {
                String[] p = sc.nextLine().split(",", -1);
                if (p.length < 7) continue;
                Vehicle v = p[1].equals("Car") ? new Car(p[0], p[2]) : new Motorcycle(p[0], p[2]);
                int slot = Integer.parseInt(p[3]);
                Date in = sdf.parse(p[4]);
                ParkingRecord rec = new ParkingRecord(v, slot, in);
                rec.setBookingId(p[6]);
                if (!p[5].equals("ACTIVE")) {
                    rec.checkout(sdf.parse(p[5]));
                } else {
                    ParkingSlot s = lot.getSlot(slot);
                    if (s != null) s.occupy(v, in);
                }
                log.add(rec);
            }
        } catch (Exception e) { System.err.println("Load error: " + e.getMessage()); }
    }
}

// ============================================================
//  GUI  — modern dark dashboard
// ============================================================
class ParkingGUI {
    private final ParkingManager manager;
    private JFrame mainFrame;

    // Stat labels updated after every action
    private JLabel statTotal, statFree, statOccupied;

    ParkingGUI(ParkingManager manager) { this.manager = manager; }

    // ─── HELPERS ────────────────────────────────────────────

    private JFrame baseFrame(String title, int w, int h) {
        JFrame f = new JFrame(title);
        f.setSize(w, h);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { f.dispose(); showMainMenu(); }
        });
        f.getContentPane().setBackground(Theme.BG);
        return f;
    }

    private JLabel hdr(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.TITLE);
        l.setForeground(Theme.TEXT);
        return l;
    }

    private JLabel sub(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private JPanel sep() {
        JPanel p = new JPanel();
        p.setBackground(Theme.BORDER);
        p.setPreferredSize(new Dimension(1, 1));
        return p;
    }

    private void showMsg(Component parent, String title, String msg, int type) {
        UIManager.put("OptionPane.background",      Theme.SURFACE);
        UIManager.put("Panel.background",           Theme.SURFACE);
        UIManager.put("OptionPane.messageForeground",Theme.TEXT);
        UIManager.put("OptionPane.messageFont",     Theme.BODY);
        JOptionPane.showMessageDialog(parent, msg, title, type);
    }

    private void updateStats() {
        ParkingLot lot = manager.getParkingLot();
        statTotal.setText(String.valueOf(lot.getTotalSlots()));
        statFree .setText(String.valueOf(lot.getAvailableCount()));
        statOccupied.setText(String.valueOf(lot.getOccupiedSlots().size()));
    }

    // ─── STAT CARD ──────────────────────────────────────────

    private JPanel statCard(String label, JLabel valueLabel, Color accent) {
        RoundPanel card = new RoundPanel(14);
        card.setBackground(Theme.SURFACE);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        valueLabel.setForeground(accent);

        card.add(lbl,        BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ─── SLOT GRID ──────────────────────────────────────────

    private JPanel slotGrid() {
        JPanel grid = new JPanel(new GridLayout(4, 5, 8, 8));
        grid.setOpaque(false);
        ParkingLot lot = manager.getParkingLot();

        for (ParkingSlot slot : lot.getAllSlots()) {
            RoundPanel cell = new RoundPanel(10);
            cell.setLayout(new BorderLayout(2, 2));
            cell.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 6));

            JLabel num = new JLabel("S" + slot.getSlotNumber(), JLabel.CENTER);
            num.setFont(new Font("SansSerif", Font.BOLD, 11));

            if (slot.isOccupied()) {
                cell.setBackground(new Color(35, 50, 35));
                num.setForeground(Theme.ACCENT2);
                JLabel ico = new JLabel(slot.getOccupant().getType().equals("Car") ? "🚗" : "🏍", JLabel.CENTER);
                ico.setFont(new Font("SansSerif", Font.PLAIN, 18));
                JLabel plate = new JLabel(slot.getOccupant().getPlateNumber(), JLabel.CENTER);
                plate.setFont(new Font("SansSerif", Font.BOLD, 9));
                plate.setForeground(Theme.TEXT_MUTED);
                cell.add(num,   BorderLayout.NORTH);
                cell.add(ico,   BorderLayout.CENTER);
                cell.add(plate, BorderLayout.SOUTH);
            } else {
                cell.setBackground(Theme.SURFACE2);
                num.setForeground(Theme.TEXT_MUTED);
                JLabel free = new JLabel("FREE", JLabel.CENTER);
                free.setFont(new Font("SansSerif", Font.BOLD, 9));
                free.setForeground(new Color(50, 80, 50));
                cell.add(num,  BorderLayout.NORTH);
                cell.add(free, BorderLayout.CENTER);
            }
            grid.add(cell);
        }
        return grid;
    }

    // ─── MAIN MENU ──────────────────────────────────────────

    public void showMainMenu() {
        if (mainFrame != null) { mainFrame.dispose(); }

        mainFrame = new JFrame("ParkEase — Parking Management System");
        mainFrame.setSize(820, 680);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.getContentPane().setBackground(Theme.BG);
        mainFrame.setLayout(new BorderLayout(0, 0));

        // ── Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.SURFACE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(16, 28, 16, 28)));

        JLabel logo = new JLabel("🅿  ParkEase");
        logo.setFont(new Font("SansSerif", Font.BOLD, 20));
        logo.setForeground(Theme.ACCENT2);

        //JLabel version = new JLabel("v2.0  |  20 Slots");
        //version.setFont(Theme.SMALL);
        //version.setForeground(Theme.TEXT_MUTED);

        topBar.add(logo,    BorderLayout.WEST);
        //topBar.add(version, BorderLayout.EAST);
        mainFrame.add(topBar, BorderLayout.NORTH);

        // ── Centre split: left nav + right content
        JPanel centre = new JPanel(new BorderLayout(20, 0));
        centre.setOpaque(false);
        centre.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Left nav
        JPanel nav = new JPanel();
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        nav.setPreferredSize(new Dimension(190, 0));

        JLabel navTitle = new JLabel("ACTIONS");
        navTitle.setFont(new Font("SansSerif", Font.BOLD, 10));
        navTitle.setForeground(Theme.TEXT_MUTED);
        navTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(navTitle);
        nav.add(Box.createVerticalStrut(10));

        String[][] navItems = {
            {"🚘  Check In",       "checkIn"},
            {"🚪  Check Out",      "checkOut"},
            {"📋  Active Bookings","active"},
            {"❌  Cancel Booking", "cancel"},
            {"📊  Slot Overview",  "summary"},
            {"📜  History",        "history"},
        };

        for (String[] item : navItems) {
            JButton btn = navBtn(item[0]);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            String action = item[1];
            btn.addActionListener(e -> dispatch(action));
            nav.add(btn);
            nav.add(Box.createVerticalStrut(6));
        }

        nav.add(Box.createVerticalGlue());
        JButton exitBtn = new FancyButton("  ⏻  Exit", Theme.DANGER, new Color(200, 40, 40));
        exitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        exitBtn.setPreferredSize(new Dimension(180, 38));
        exitBtn.setMaximumSize(new Dimension(180, 38));
        exitBtn.addActionListener(e -> System.exit(0));
        nav.add(exitBtn);

        // Right content
        JPanel right = new JPanel(new BorderLayout(0, 16));
        right.setOpaque(false);

        // Stats row
        statTotal    = new JLabel("20");
        statFree     = new JLabel("0");
        statOccupied = new JLabel("0");
        updateStats();

        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("Total Slots",    statTotal,    Theme.HIGHLIGHT));
        stats.add(statCard("Available",      statFree,     Theme.ACCENT2));
        stats.add(statCard("Occupied",       statOccupied, Theme.WARN));

        // Slot grid
        RoundPanel gridPanel = new RoundPanel(14);
        gridPanel.setBackground(Theme.SURFACE);
        gridPanel.setLayout(new BorderLayout(0, 12));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel gridTitle = new JLabel("Live Slot Map");
        gridTitle.setFont(Theme.HEAD);
        gridTitle.setForeground(Theme.TEXT);

        gridPanel.add(gridTitle,  BorderLayout.NORTH);
        gridPanel.add(slotGrid(), BorderLayout.CENTER);

        right.add(stats,     BorderLayout.NORTH);
        right.add(gridPanel, BorderLayout.CENTER);

        centre.add(nav,   BorderLayout.WEST);
        centre.add(right, BorderLayout.CENTER);
        mainFrame.add(centre, BorderLayout.CENTER);

        mainFrame.setVisible(true);
    }

    private JButton navBtn(String text) {
        JButton btn = new JButton(text) {
            private boolean h = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { h=true; repaint(); }
                public void mouseExited (MouseEvent e) { h=false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (h) { g2.setColor(Theme.SURFACE2); g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8); }
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setFont(Theme.BODY);
        btn.setForeground(Theme.TEXT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 38));
        btn.setMaximumSize  (new Dimension(180, 38));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        return btn;
    }

    private void dispatch(String action) {
        switch (action) {
            case "checkIn":  showCheckIn();  break;
            case "checkOut": showCheckOut(); break;
            case "active":   showActive();   break;
            case "cancel":   showCancel();   break;
            case "summary":  showSummary();  break;
            case "history":  showHistory();  break;
        }
    }

    private void refreshMain() {
        updateStats();
        // Rebuild slot grid
        Component centre = ((BorderLayout)mainFrame.getContentPane().getLayout())
                           .getLayoutComponent(BorderLayout.CENTER);
        mainFrame.revalidate();
        mainFrame.repaint();
        // Full refresh is simplest:
        mainFrame.dispose();
        mainFrame = null;
        showMainMenu();
    }

    // ─── CHECK IN ───────────────────────────────────────────

    private void showCheckIn() {
        JFrame f = baseFrame("Check In Vehicle", 480, 420);
        f.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 20));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        // Header
        JPanel hdrP = new JPanel(new BorderLayout());
        hdrP.setOpaque(false);
        hdrP.add(hdr("Check In"), BorderLayout.WEST);
        hdrP.add(sub("Register a new vehicle"), BorderLayout.SOUTH);
        main.add(hdrP, BorderLayout.NORTH);

        // Form
        RoundPanel form = new RoundPanel(14);
        form.setBackground(Theme.SURFACE);
        form.setLayout(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 0, 6, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        DarkField ownerField = new DarkField(20);
        DarkField plateField = new DarkField(20);
        DarkCombo typeCombo  = new DarkCombo(new String[]{"Car", "Motorcycle"});

        addFormRow(form, gc, 0, "Owner Name",   ownerField);
        addFormRow(form, gc, 1, "Plate Number", plateField);
        addFormRow(form, gc, 2, "Vehicle Type", typeCombo);

        // Buttons
        gc.gridy = 3; gc.gridx = 0; gc.gridwidth = 2;
        gc.insets = new Insets(18, 0, 0, 0);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        FancyButton cancel = new FancyButton("Cancel", Theme.SURFACE2, Theme.BORDER);
        FancyButton submit = new FancyButton("Check In  →", Theme.ACCENT, Theme.ACCENT2);
        cancel.setPreferredSize(new Dimension(110, 40));
        submit.setPreferredSize(new Dimension(140, 40));
        btns.add(cancel); btns.add(submit);
        form.add(btns, gc);

        main.add(form, BorderLayout.CENTER);
        f.add(main);

        submit.addActionListener(e -> {
            String result = manager.checkIn(plateField.getText().trim(),
                                             ownerField.getText().trim(),
                                             (String) typeCombo.getSelectedItem());
            if (result.startsWith("SUCCESS")) {
                String[] p = result.split("\\|");
                showReceiptDialog(f, "✅  Checked In Successfully",
                    "Slot Assigned : " + p[1] + "\n" +
                    "Booking ID    : " + p[2] + "\n" +
                    "Vehicle       : " + plateField.getText().trim() + "\n" +
                    "Owner         : " + ownerField.getText().trim(),
                    Theme.ACCENT2);
                f.dispose(); refreshMain();
            } else {
                showMsg(f, "Error", result, JOptionPane.ERROR_MESSAGE);
            }
        });
        cancel.addActionListener(e -> f.dispose());

        f.setVisible(true);
    }

    // ─── CHECK OUT ──────────────────────────────────────────

    private void showCheckOut() {
        List<ParkingSlot> occupied = manager.getParkingLot().getOccupiedSlots();
        if (occupied.isEmpty()) { showMsg(mainFrame,"Info","No vehicles currently parked.",JOptionPane.INFORMATION_MESSAGE); return; }

        JFrame f = baseFrame("Check Out Vehicle", 580, 480);
        f.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel hdrP = new JPanel(new BorderLayout());
        hdrP.setOpaque(false);
        hdrP.add(hdr("Check Out"), BorderLayout.WEST);
        hdrP.add(sub("Select a vehicle to check out"), BorderLayout.SOUTH);
        main.add(hdrP, BorderLayout.NORTH);

        // Table
        String[] cols = {"Slot","Type","Plate","Owner","Parked For","Est. Fee"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        for (ParkingSlot s : occupied) {
            long mins = s.getOccupancyDurationMinutes();
            long hrs  = mins / 60; long m = mins % 60;
            double fee = s.getOccupant().calculateFee(mins);
            model.addRow(new Object[]{
                "S" + s.getSlotNumber(),
                s.getOccupant().getType(),
                s.getOccupant().getPlateNumber(),
                s.getOccupant().getOwner(),
                hrs + "h " + m + "m",
                String.format("PKR %.0f", fee)
            });
        }

        JTable table = styledTable(model);
        JScrollPane scroll = darkScroll(table);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        FancyButton back    = new FancyButton("Back", Theme.SURFACE2, Theme.BORDER);
        FancyButton checkout= new FancyButton("Check Out  →", Theme.ACCENT, Theme.ACCENT2);
        back.setPreferredSize(new Dimension(100, 40));
        checkout.setPreferredSize(new Dimension(150, 40));
        btns.add(back); btns.add(checkout);

        main.add(scroll, BorderLayout.CENTER);
        main.add(btns,   BorderLayout.SOUTH);
        f.add(main);

        checkout.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showMsg(f,"Info","Please select a vehicle.",JOptionPane.WARNING_MESSAGE); return; }
            int slotNum = occupied.get(row).getSlotNumber();
            String result = manager.checkOut(slotNum);
            if (result.startsWith("SUCCESS")) {
                String[] p = result.split("\\|");
                long dur = Long.parseLong(p[5].equals("") ? "0" : p[4]);
                showReceiptDialog(f, "🧾  Checkout Receipt",
                    "Plate     : " + p[1] + "\n" +
                    "Owner     : " + p[2] + "\n" +
                    "Type      : " + p[3] + "\n" +
                    "Duration  : " + p[4] + " minutes\n" +
                    "Fee Paid  : PKR " + p[5] + "\n" +
                    "Booking   : " + p[6],
                    Theme.HIGHLIGHT);
                f.dispose(); refreshMain();
            } else {
                showMsg(f,"Error", result, JOptionPane.ERROR_MESSAGE);
            }
        });
        back.addActionListener(e -> f.dispose());

        f.setVisible(true);
    }

    // ─── ACTIVE BOOKINGS ────────────────────────────────────

    private void showActive() {
        List<ParkingRecord> active = manager.activeRecords();

        JFrame f = baseFrame("Active Bookings", 700, 420);
        f.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel hdrP = new JPanel(new BorderLayout());
        hdrP.setOpaque(false);
        hdrP.add(hdr("Active Bookings"), BorderLayout.WEST);
        hdrP.add(sub(active.size() + " vehicle(s) currently parked"), BorderLayout.SOUTH);
        main.add(hdrP, BorderLayout.NORTH);

        String[] cols = {"Booking ID","Slot","Type","Plate","Owner","Check-In","Duration","Live Fee"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm");
        for (ParkingRecord r : active) {
            long mins = r.getDurationMinutes();
            double fee = r.getVehicle().calculateFee(mins);
            model.addRow(new Object[]{
                r.getBookingId(), "S" + r.getSlotNumber(),
                r.getVehicle().getType(), r.getVehicle().getPlateNumber(),
                r.getVehicle().getOwner(), sdf.format(r.getCheckInTime()),
                (mins/60) + "h " + (mins%60) + "m",
                String.format("PKR %.0f", fee)
            });
        }

        JTable table = styledTable(model);
        main.add(darkScroll(table), BorderLayout.CENTER);

        FancyButton back = new FancyButton("← Back", Theme.SURFACE2, Theme.BORDER);
        back.setPreferredSize(new Dimension(120, 40));
        back.addActionListener(e -> f.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.setOpaque(false); bp.add(back);
        main.add(bp, BorderLayout.SOUTH);

        f.add(main);
        f.setVisible(true);
    }

    // ─── CANCEL BOOKING ─────────────────────────────────────

    private void showCancel() {
        List<ParkingRecord> active = manager.activeRecords();
        if (active.isEmpty()) { showMsg(mainFrame,"Info","No active bookings to cancel.",JOptionPane.INFORMATION_MESSAGE); return; }

        JFrame f = baseFrame("Cancel Booking", 620, 400);
        f.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel hdrP = new JPanel(new BorderLayout());
        hdrP.setOpaque(false);
        hdrP.add(hdr("Cancel Booking"), BorderLayout.WEST);
        hdrP.add(sub("Select a booking to cancel (no fee charged)"), BorderLayout.SOUTH);
        main.add(hdrP, BorderLayout.NORTH);

        String[] cols = {"Booking ID","Slot","Plate","Owner","Type"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (ParkingRecord r : active)
            model.addRow(new Object[]{ r.getBookingId(), "S"+r.getSlotNumber(),
                r.getVehicle().getPlateNumber(), r.getVehicle().getOwner(), r.getVehicle().getType() });

        JTable table = styledTable(model);
        main.add(darkScroll(table), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        FancyButton back   = new FancyButton("Back", Theme.SURFACE2, Theme.BORDER);
        FancyButton cancel = new FancyButton("Cancel Booking", Theme.DANGER, new Color(180,40,40));
        back.setPreferredSize(new Dimension(110,40)); cancel.setPreferredSize(new Dimension(160,40));
        btns.add(back); btns.add(cancel);
        main.add(btns, BorderLayout.SOUTH);

        cancel.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { showMsg(f,"Info","Select a booking first.",JOptionPane.WARNING_MESSAGE); return; }
            String id = active.get(row).getBookingId();
            int confirm = JOptionPane.showConfirmDialog(f,"Cancel booking "+id+"?","Confirm",JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            String result = manager.cancel(id);
            showMsg(f, result.startsWith("SUCCESS") ? "Done" : "Error", result,
                    result.startsWith("SUCCESS") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            f.dispose(); refreshMain();
        });
        back.addActionListener(e -> f.dispose());

        f.add(main);
        f.setVisible(true);
    }

    // ─── SLOT SUMMARY ───────────────────────────────────────

    private void showSummary() {
        JFrame f = baseFrame("Slot Overview", 700, 540);
        f.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel hdrP = new JPanel(new BorderLayout());
        hdrP.setOpaque(false);
        hdrP.add(hdr("Slot Overview"), BorderLayout.WEST);
        hdrP.add(sub("Real-time map of all parking slots"), BorderLayout.SOUTH);
        main.add(hdrP, BorderLayout.NORTH);

        // Big slot grid
        JPanel grid = new JPanel(new GridLayout(4, 5, 10, 10));
        grid.setOpaque(false);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        for (ParkingSlot slot : manager.getParkingLot().getAllSlots()) {
            RoundPanel cell = new RoundPanel(12);
            cell.setLayout(new BorderLayout(0, 4));
            cell.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel numLbl = new JLabel("Slot " + slot.getSlotNumber(), JLabel.CENTER);
            numLbl.setFont(new Font("SansSerif", Font.BOLD, 11));

            if (slot.isOccupied()) {
                cell.setBackground(new Color(30, 50, 30));
                numLbl.setForeground(Theme.ACCENT2);
                long mins = slot.getOccupancyDurationMinutes();
                JLabel ico   = new JLabel(slot.getOccupant().getType().equals("Car") ? "🚗" : "🏍", JLabel.CENTER);
                ico.setFont(new Font("SansSerif", Font.PLAIN, 22));
                JLabel plate = new JLabel(slot.getOccupant().getPlateNumber(), JLabel.CENTER);
                plate.setFont(new Font("SansSerif", Font.BOLD, 10));
                plate.setForeground(Theme.TEXT);
                JLabel dur   = new JLabel((mins/60)+"h "+(mins%60)+"m", JLabel.CENTER);
                dur.setFont(new Font("SansSerif", Font.PLAIN, 9));
                dur.setForeground(Theme.TEXT_MUTED);
                JPanel info = new JPanel(new GridLayout(3,1,0,1));
                info.setOpaque(false);
                info.add(ico); info.add(plate); info.add(dur);
                cell.add(numLbl, BorderLayout.NORTH);
                cell.add(info,   BorderLayout.CENTER);
            } else {
                cell.setBackground(Theme.SURFACE2);
                numLbl.setForeground(Theme.TEXT_MUTED);
                JLabel free = new JLabel("FREE", JLabel.CENTER);
                free.setFont(new Font("SansSerif", Font.BOLD, 11));
                free.setForeground(new Color(46, 100, 46));
                cell.add(numLbl, BorderLayout.NORTH);
                cell.add(free,   BorderLayout.CENTER);
            }
            grid.add(cell);
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        main.add(scroll, BorderLayout.CENTER);

        FancyButton back = new FancyButton("← Back", Theme.SURFACE2, Theme.BORDER);
        back.setPreferredSize(new Dimension(120,40));
        back.addActionListener(e -> f.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.setOpaque(false); bp.add(back);
        main.add(bp, BorderLayout.SOUTH);

        f.add(main);
        f.setVisible(true);
    }

    // ─── HISTORY ────────────────────────────────────────────

    private void showHistory() {
        List<ParkingRecord> all = manager.allRecords();

        JFrame f = baseFrame("Parking History", 760, 460);
        f.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel hdrP = new JPanel(new BorderLayout());
        hdrP.setOpaque(false);
        hdrP.add(hdr("Parking History"), BorderLayout.WEST);
        hdrP.add(sub(all.size() + " total record(s)"), BorderLayout.SOUTH);
        main.add(hdrP, BorderLayout.NORTH);

        String[] cols = {"Booking ID","Slot","Type","Plate","Owner","Check-In","Check-Out","Duration","Fee","Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm");
        for (ParkingRecord r : all) {
            long mins = r.getDurationMinutes();
            String out = r.getCheckOutTime() != null ? sdf.format(r.getCheckOutTime()) : "—";
            model.addRow(new Object[]{
                r.getBookingId(), "S"+r.getSlotNumber(),
                r.getVehicle().getType(), r.getVehicle().getPlateNumber(),
                r.getVehicle().getOwner(), sdf.format(r.getCheckInTime()), out,
                (mins/60)+"h "+(mins%60)+"m",
                r.isActive() ? "TBD" : String.format("PKR %.0f", r.getFeePaid()),
                r.isActive() ? "ACTIVE" : "DONE"
            });
        }

        JTable table = styledTable(model);
        // Color status column
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                setForeground("ACTIVE".equals(v) ? Theme.ACCENT2 : Theme.TEXT_MUTED);
                return this;
            }
        });

        main.add(darkScroll(table), BorderLayout.CENTER);

        FancyButton back = new FancyButton("← Back", Theme.SURFACE2, Theme.BORDER);
        back.setPreferredSize(new Dimension(120, 40));
        back.addActionListener(e -> f.dispose());
        JPanel bp = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bp.setOpaque(false); bp.add(back);
        main.add(bp, BorderLayout.SOUTH);

        f.add(main);
        f.setVisible(true);
    }

    // ─── RECEIPT DIALOG ─────────────────────────────────────

    private void showReceiptDialog(Component parent, String title, String body, Color accent) {
        RoundPanel panel = new RoundPanel(14);
        panel.setBackground(Theme.SURFACE);
        panel.setLayout(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        panel.setPreferredSize(new Dimension(380, 260));

        JLabel ttl = new JLabel(title);
        ttl.setFont(Theme.HEAD);
        ttl.setForeground(accent);

        JTextArea ta = new JTextArea(body);
        ta.setFont(Theme.MONO);
        ta.setForeground(Theme.TEXT);
        ta.setBackground(Theme.SURFACE2);
        ta.setEditable(false);
        ta.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        RoundPanel inner = new RoundPanel(8);
        inner.setBackground(Theme.SURFACE2);
        inner.setLayout(new BorderLayout());
        inner.add(ta);

        panel.add(ttl,   BorderLayout.NORTH);
        panel.add(inner, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(parent, panel, "ParkEase", JOptionPane.PLAIN_MESSAGE);
    }

    // ─── TABLE HELPERS ──────────────────────────────────────

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(Theme.SURFACE);
        t.setForeground(Theme.TEXT);
        t.setFont(Theme.BODY);
        t.setRowHeight(34);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(35, 80, 55));
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader hdr = t.getTableHeader();
        hdr.setBackground(Theme.SURFACE2);
        hdr.setForeground(Theme.TEXT_MUTED);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 11));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        // Alternating row renderer
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.SURFACE2);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
        return t;
    }

    private JScrollPane darkScroll(JTable table) {
        JScrollPane s = new JScrollPane(table);
        s.setOpaque(false);
        s.getViewport().setBackground(Theme.SURFACE);
        s.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        return s;
    }

    private void addFormRow(JPanel p, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridy = row; gc.gridx = 0; gc.gridwidth = 1; gc.weightx = 0.3;
        gc.insets = new Insets(8, 0, 8, 12);
        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.BODY);
        lbl.setForeground(Theme.TEXT_MUTED);
        p.add(lbl, gc);

        gc.gridx = 1; gc.weightx = 0.7; gc.insets = new Insets(8, 0, 8, 0);
        if (field instanceof DarkCombo) {
            field.setPreferredSize(new Dimension(200, 38));
        }
        p.add(field, gc);
    }
}
