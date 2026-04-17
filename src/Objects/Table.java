package Objects;

import java.util.ArrayList;
import java.util.List;

public class Table {
    private String name;
    private List<Field> fields = new ArrayList<>();

    public Table(String name){
        this.name = name;
    }

    public void addField(Field field){
        fields.add(field);
    }
}
