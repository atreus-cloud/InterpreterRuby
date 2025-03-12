import java.io.*;
import java.util.*;

enum TokenType {
    NUMBER, IDENTIFIER, ASSIGN, PLUS, MINUS, MULT, DIV, LPAREN, RPAREN, PRINT, END, IF, ELSE, THEN, ENDIF, LT, GT, EQ, UNKNOWN
}

class Token {
    TokenType type;
    String value;
    
    Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }
}

class Lexer {
    private String input;
    private int pos;

    Lexer(String input) {
        this.input = input;
        this.pos = 0;
    }

    private char peek() {
        return pos < input.length() ? input.charAt(pos) : '\0';
    }

    private char advance() {
        return pos < input.length() ? input.charAt(pos++) : '\0';
    }

    List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char current = peek();
            if (Character.isWhitespace(current)) {
                advance();
            } else if (Character.isDigit(current)) {
                StringBuilder number = new StringBuilder();
                while (Character.isDigit(peek())) {
                    number.append(advance());
                }
                tokens.add(new Token(TokenType.NUMBER, number.toString()));
            } else if (Character.isLetter(current)) {
                StringBuilder identifier = new StringBuilder();
                while (Character.isLetter(peek())) {
                    identifier.append(advance());
                }
                String text = identifier.toString();
                switch (text) {
                    case "print" -> tokens.add(new Token(TokenType.PRINT, text));
                    case "if" -> tokens.add(new Token(TokenType.IF, text));
                    case "else" -> tokens.add(new Token(TokenType.ELSE, text));
                    case "end" -> tokens.add(new Token(TokenType.ENDIF, text));
                    default -> tokens.add(new Token(TokenType.IDENTIFIER, text));
                }
            } else {
                switch (current) {
                    case '=' -> tokens.add(new Token(TokenType.ASSIGN, "="));
                    case '+' -> tokens.add(new Token(TokenType.PLUS, "+"));
                    case '-' -> tokens.add(new Token(TokenType.MINUS, "-"));
                    case '*' -> tokens.add(new Token(TokenType.MULT, "*"));
                    case '/' -> tokens.add(new Token(TokenType.DIV, "/"));
                    case '(' -> tokens.add(new Token(TokenType.LPAREN, "("));
                    case ')' -> tokens.add(new Token(TokenType.RPAREN, ")"));
                    case '<' -> tokens.add(new Token(TokenType.LT, "<"));
                    case '>' -> tokens.add(new Token(TokenType.GT, ">"));
                    default -> tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(current)));
                }
                advance();
            }
        }
        tokens.add(new Token(TokenType.END, ""));
        return tokens;
    }
}

abstract class ASTNode {}

class BinaryOp extends ASTNode {
    ASTNode left, right;
    TokenType operator;

    BinaryOp(ASTNode left, TokenType operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}

class NumberNode extends ASTNode {
    int value;
    NumberNode(int value) { this.value = value; }
}

class VarNode extends ASTNode {
    String name;
    VarNode(String name) { this.name = name; }
}

class AssignNode extends ASTNode {
    String name;
    ASTNode value;
    AssignNode(String name, ASTNode value) {
        this.name = name;
        this.value = value;
    }
}

class PrintNode extends ASTNode {
    ASTNode value;
    PrintNode(ASTNode value) { this.value = value; }
}

class IfNode extends ASTNode {
    ASTNode condition;
    List<ASTNode> trueBranch, falseBranch;
    IfNode(ASTNode condition, List<ASTNode> trueBranch, List<ASTNode> falseBranch) {
        this.condition = condition;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }
}

class Parser {
    private List<Token> tokens;
    private int pos = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token eat(TokenType type) {
        if (pos < tokens.size() && tokens.get(pos).type == type) {
            return tokens.get(pos++);
        }
        throw new RuntimeException("Unexpected token: " + tokens.get(pos));
    }

    private ASTNode factor() {
        Token token = tokens.get(pos++);
        if (token.type == TokenType.NUMBER) {
            return new NumberNode(Integer.parseInt(token.value));
        } else if (token.type == TokenType.IDENTIFIER) {
            return new VarNode(token.value);
        }
        throw new RuntimeException("Unexpected token in factor: " + token);
    }

    private ASTNode expression() {
        ASTNode left = factor();
        while (pos < tokens.size() && (tokens.get(pos).type == TokenType.PLUS || tokens.get(pos).type == TokenType.MINUS)) {
            TokenType operator = tokens.get(pos++).type;
            ASTNode right = factor();
            left = new BinaryOp(left, operator, right);
        }
        return left;
    }

    public List<ASTNode> program() {
        List<ASTNode> statements = new ArrayList<>();
        while (tokens.get(pos).type != TokenType.END) {
            Token token = tokens.get(pos);
            if (token.type == TokenType.IF) {
                eat(TokenType.IF);
                ASTNode condition = expression();
                List<ASTNode> trueBranch = program();
                List<ASTNode> falseBranch = new ArrayList<>();
                if (tokens.get(pos).type == TokenType.ELSE) {
                    eat(TokenType.ELSE);
                    falseBranch = program();
                }
                eat(TokenType.ENDIF);
                statements.add(new IfNode(condition, trueBranch, falseBranch));
            } else if (token.type == TokenType.PRINT) {
                eat(TokenType.PRINT);
                statements.add(new PrintNode(expression()));
            } else {
                String varName = eat(TokenType.IDENTIFIER).value;
                eat(TokenType.ASSIGN);
                statements.add(new AssignNode(varName, expression()));
            }
        }
        return statements;
    }
}

class Interpreter {
    private Map<String, Integer> variables = new HashMap<>();

    void execute(List<ASTNode> ast) {
        for (ASTNode node : ast) {
            if (node instanceof AssignNode assignNode) {
                variables.put(assignNode.name, evaluate(assignNode.value));
            } else if (node instanceof PrintNode printNode) {
                System.out.println(evaluate(printNode.value));
            }
        }
    }

    private int evaluate(ASTNode node) {
        if (node instanceof NumberNode numberNode) {
            return numberNode.value;
        } else if (node instanceof VarNode varNode) {
            return variables.getOrDefault(varNode.name, 0);
        }
        throw new RuntimeException("Unknown expression");
    }
}

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Main <filename>");
            return;
        }

        String filename = args[0];
        StringBuilder code = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                code.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        Lexer lexer = new Lexer(code.toString());
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        List<ASTNode> ast = parser.program();
        Interpreter interpreter = new Interpreter();
        interpreter.execute(ast);
    }
}
