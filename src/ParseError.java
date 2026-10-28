public record ParseError(int lineNumber, String rawLine, String reason) {

    @Override
    public String toString() {
        return rawLine + " — " + reason;
    }
}