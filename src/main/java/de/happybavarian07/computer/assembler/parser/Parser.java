package de.happybavarian07.computer.assembler.parser;

import de.happybavarian07.computer.assembler.parser.model.Program;
import de.happybavarian07.computer.assembler.parser.model.Statement;

public interface Parser {
    Program parse();

    Statement parseStatement();

    void reset(String source, String filePath);
}
