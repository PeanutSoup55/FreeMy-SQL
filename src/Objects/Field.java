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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
