package Objects;

public class Field {
    private String name;
    private String type;
    private boolean isPrimary;
    private String reference;

    public Field(String reference, boolean isPrimary, String type, String name) {
        this.reference = reference;
        this.isPrimary = isPrimary;
        this.type = type;
        this.name = name;
    }
}
