import java.io.*;
import java.util.*;

enum TokenType {
    NUMBER, IDENTIFIER, ASSIGN, PLUS, MINUS, MULT, DIV, LPAREN, RPAREN, PRINT, END, UNKNOWN
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
                if (text.equals("print")) {
                    tokens.add(new Token(TokenType.PRINT, text));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, text));
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

class NumberNode extends ASTNode {
    int value;
    NumberNode(int value) { this.value = value; }
}

class VariableNode extends ASTNode {
    String name;
    VariableNode(String name) { this.name = name; }
}

class BinaryOpNode extends ASTNode {
    ASTNode left, right;
    TokenType operator;
    BinaryOpNode(ASTNode left, TokenType operator, ASTNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
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

class Parser {
    private List<Token> tokens;
    private int pos;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    private Token consume() {
        return tokens.get(pos++);
    }

    private Token peek() {
        return tokens.get(pos);
    }

    ASTNode parseExpression() {
        Token token = consume();
        if (token.type == TokenType.NUMBER) {
            return new NumberNode(Integer.parseInt(token.value));
        } else if (token.type == TokenType.IDENTIFIER) {
            return new VariableNode(token.value);
        } else {
            throw new RuntimeException("Unexpected token: " + token.value);
        }
    }

    ASTNode parseStatement() {
        Token token = peek();
        if (token.type == TokenType.PRINT) {
            consume();
            return new PrintNode(parseExpression());
        } else if (token.type == TokenType.IDENTIFIER) {
            consume();
            Token assign = consume();
            if (assign.type == TokenType.ASSIGN) {
                return new AssignNode(token.value, parseExpression());
            }
        }
        throw new RuntimeException("Invalid statement");
    }
}

class Interpreter {
    private Map<String, Integer> variables = new HashMap<>();

    void interpret(ASTNode node) {
        if (node instanceof NumberNode numNode) {
            System.out.println(numNode.value);
        } else if (node instanceof VariableNode varNode) {
            System.out.println(variables.getOrDefault(varNode.name, 0));
        } else if (node instanceof BinaryOpNode binNode) {
            int left = evaluate(binNode.left);
            int right = evaluate(binNode.right);
            switch (binNode.operator) {
                case PLUS -> System.out.println(left + right);
                case MINUS -> System.out.println(left - right);
                case MULT -> System.out.println(left * right);
                case DIV -> System.out.println(left / right);
            }
        } else if (node instanceof AssignNode assignNode) {
            variables.put(assignNode.name, evaluate(assignNode.value));
        } else if (node instanceof PrintNode printNode) {
            System.out.println(evaluate(printNode.value));
        }
    }

    private int evaluate(ASTNode node) {
        if (node instanceof NumberNode numNode) {
            return numNode.value;
        } else if (node instanceof VariableNode varNode) {
            return variables.getOrDefault(varNode.name, 0);
        } else if (node instanceof BinaryOpNode binNode) {
            int left = evaluate(binNode.left);
            int right = evaluate(binNode.right);
            return switch (binNode.operator) {
                case PLUS -> left + right;
                case MINUS -> left - right;
                case MULT -> left * right;
                case DIV -> left / right;
                default -> 0;
            };
        }
        return 0;
    }
}

public class interpreter {
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
        ASTNode ast = parser.parseStatement();
        Interpreter interpreter = new Interpreter();
        interpreter.interpret(ast);
    }
}
