package app.awqat.prayer;

/** The 20 Tasbeeh phrases. No repetition count or reward is implied; the target is chosen by the user. */
public final class TasbeehData {
    private TasbeehData() {}

    public static final String[] PHRASES = {
        "سبحان الله",
        "الحمد لله",
        "الله أكبر",
        "لا إله إلا الله",
        "أستغفر الله",
        "سبحان الله وبحمده",
        "سبحان الله العظيم",
        "لا حول ولا قوة إلا بالله",
        "لا إله إلا الله وحده لا شريك له",
        "اللهم صل وسلم على نبينا محمد",
        "أستغفر الله وأتوب إليه",
        "حسبي الله ونعم الوكيل",
        "توكلت على الله",
        "سبحان الله والحمد لله",
        "سبحان الله والحمد لله ولا إله إلا الله والله أكبر",
        "اللهم اغفر لي",
        "رب اغفر لي وتب علي",
        "لا إله إلا أنت سبحانك إني كنت من الظالمين",
        "رضيت بالله ربًا وبالإسلام دينًا وبمحمد ﷺ نبيًا",
        "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير",
    };

    public static final int[] TARGET_CHOICES = {33, 99, 100};
    public static final int DEFAULT_TARGET = 33;
}
