package app.awqat.prayer;

/** One morning/evening remembrance. {@code type}: quran or prophetic. */
public final class Dhikr {
    public final String id, period, text, type, source, grade, countNote;
    public final int count;

    public Dhikr(String id, String period, String text, int count, String type, String source, String grade,
                 String countNote) {
        this.id = id; this.period = period; this.text = text; this.count = count; this.type = type;
        this.source = source; this.grade = grade; this.countNote = countNote;
    }

    public String typeLabel() { return "quran".equals(type) ? "من القرآن الكريم" : "ذكر نبوي"; }
}
