import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Lexer {

    enum Tok {
        L_LLAVE, R_LLAVE, L_CORCHETE, R_CORCHETE, COMA, DOS_PUNTOS,
        STRING, NUMBER, PR_TRUE, PR_FALSE, PR_NULL
    }

    static class LineLexer {
        private final String s;
        private int i = 0;
        private final int n;

        LineLexer(String line) { this.s = line; this.n = line.length(); }

        private void skipWS() {
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        }

        List<String> tokenize() throws LexException {
            List<String> out = new ArrayList<>();
            while (true) {
                skipWS();
                if (i >= n) break;
                char c = s.charAt(i);
                switch (c) {
                    case '{': out.add(Tok.L_LLAVE.name()); i++; break;
                    case '}': out.add(Tok.R_LLAVE.name()); i++; break;
                    case '[': out.add(Tok.L_CORCHETE.name()); i++; break;
                    case ']': out.add(Tok.R_CORCHETE.name()); i++; break;
                    case ',': out.add(Tok.COMA.name()); i++; break;
                    case ':': out.add(Tok.DOS_PUNTOS.name()); i++; break;
                    case '"': out.add(readString()); break;
                    default:
                        if (isDigit(c) || (c == '-' && i + 1 < n && isDigit(s.charAt(i + 1)))) {
                            out.add(readNumber());
                        } else if (isAlpha(c)) {
                            out.add(readKeyword());
                        } else {
                            throw new LexException("ERROR_LEXICO en columna " + (i + 1));
                        }
                }
            }
            return out;
        }

        private String readString() throws LexException {
            int start = i;
            i++;
            boolean closed = false;
            while (i < n) {
                char c = s.charAt(i);
                if (c == '\\') {
                    i++;
                    if (i >= n) break;
                    i++;
                } else if (c == '"') {
                    i++;
                    closed = true;
                    break;
                } else {
                    i++;
                }
            }
            if (!closed) throw new LexException("ERROR_LEXICO: string sin cerrar iniciado en col " + (start + 1));
            return Tok.STRING.name();
        }

        private String readNumber() throws LexException {
            int start = i;
            if (s.charAt(i) == '-') i++;

            boolean hasDigits = false;
            while (i < n && isDigit(s.charAt(i))) { i++; hasDigits = true; }
            if (!hasDigits) throw new LexException("ERROR_LEXICO: numero invalido " + (start + 1));

            if (i < n && s.charAt(i) == '.') {
                int dotPos = i++;
                boolean fracDigits = false;
                while (i < n && isDigit(s.charAt(i))) { i++; fracDigits = true; }
                if (!fracDigits) throw new LexException("ERROR_LEXICO: fraccion invalida " + (dotPos + 1));
            }

            if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                i++;
                if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                boolean expDigits = false;
                while (i < n && isDigit(s.charAt(i))) { i++; expDigits = true; }
                if (!expDigits) throw new LexException("ERROR_LEXICO: exponente invalido" + (i + 1));
            }
            return Tok.NUMBER.name();
        }

        private String readKeyword() throws LexException {
            int start = i;
            while (i < n && (isAlphaNum(s.charAt(i)))) i++;
            String word = s.substring(start, i);
            String lower = word.toLowerCase(Locale.ROOT);
            switch (lower) {
                case "true":  return Tok.PR_TRUE.name();
                case "false": return Tok.PR_FALSE.name();
                case "null":  return Tok.PR_NULL.name();
                default:
                    throw new LexException("ERROR_LEXICO: identificador no esperado '" + word + "'");
            }
        }

        private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
        private static boolean isAlpha(char c) { return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'); }
        private static boolean isAlphaNum(char c) { return isAlpha(c) || isDigit(c); }
    }

    static class LexException extends Exception {
        LexException(String m) { super(m); }
    }

    public static void main(String[] args) {
        File inFile = new File(args[0]);
        File outFile = new File(args[1]);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), StandardCharsets.UTF_8));
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = br.readLine()) != null) {
                try {
                    LineLexer lx = new LineLexer(line);
                    List<String> toks = lx.tokenize();
                    if (toks.isEmpty()) {
                        pw.println();
                    } else {
                        pw.println(String.join(" ", toks));
                    }
                } catch (LexException | RuntimeException ex) {
                    pw.println("ERROR_LEXICO");
                }
            }
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
            System.exit(2);
        }
    }
}
