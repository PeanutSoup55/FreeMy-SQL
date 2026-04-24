package Objects;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }
}
