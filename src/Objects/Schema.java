package Objects;

import GUI.SchemasRoot;

import java.util.ArrayList;
import java.util.List;

public class Schema {
    private String name;
    private List<Table> tables = new ArrayList<>();

    public Schema(String name){
        this.name = name;
    }

    public void addTable(Table table){
        tables.add(table);
    }
}
