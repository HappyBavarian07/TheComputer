package de.happybavarian07.computer.assembler.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
 * @Author HappyBavarian07
 * @Date August 11, 2026 | 15:25
 */
public class SymbolTable {
    private final Map<String, Integer> symbolTable;

    public SymbolTable() {
        this.symbolTable = new HashMap<>();
    }

    public void map(String name, Integer address) {
        symbolTable.putIfAbsent(name, address);
    }

    public boolean exists(String name) {
        return symbolTable.containsKey(name);
    }

    public Integer getLocation(String name) {
        return symbolTable.get(name);
    }

    public String getNameFromLocation(Integer address) {
        for (Map.Entry<String, Integer> mapEntry : symbolTable.entrySet()) {
            if (Objects.equals(address, mapEntry.getValue())) {
                return mapEntry.getKey();
            }
        }
        return null;
    }

    public void reset() {
        symbolTable.clear();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SymbolTable{");
        sb.append("symbolTable=").append(symbolTable);
        sb.append('}');
        return sb.toString();
    }
}
