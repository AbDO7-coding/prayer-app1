package app.awqat.prayer;

/** One hadith record. {@code explanation} (if any) is kept separate from the original text. */
public final class Hadith {
    public final String id, text, narrator, collection, book, number, alsoIn, edition;
    public final String gradeClass, gradeText, grader, keywords;
    public final String[] categories;
    public final String searchKey;

    public Hadith(String id, String text, String narrator, String collection, String book, String number,
                  String alsoIn, String edition, String gradeClass, String gradeText, String grader,
                  String[] categories, String keywords) {
        this.id = id; this.text = text; this.narrator = narrator; this.collection = collection; this.book = book;
        this.number = number; this.alsoIn = alsoIn; this.edition = edition; this.gradeClass = gradeClass;
        this.gradeText = gradeText; this.grader = grader; this.categories = categories; this.keywords = keywords;
        StringBuilder names = new StringBuilder();
        for (String c : categories) {
            for (int i = 0; i < HadithData.CATEGORY_CODES.length; i++) {
                if (HadithData.CATEGORY_CODES[i].equals(c)) names.append(' ').append(HadithData.CATEGORY_NAMES[i]);
            }
        }
        this.searchKey = ArabicText.normalize(text + " " + narrator + " " + collection + " " + book + " "
            + number + " " + alsoIn + " " + keywords + names);
    }

    public String reference() {
        StringBuilder b = new StringBuilder(collection);
        if (book != null && !book.isEmpty()) b.append("، ").append(book);
        b.append("، رقم ").append(number);
        if (alsoIn != null && !alsoIn.isEmpty()) b.append("، ").append(alsoIn);
        return b.toString();
    }

    public boolean inCategory(String code) {
        for (String c : categories) if (c.equals(code)) return true;
        return false;
    }
}
