package freemarker.core;

/**
 * Class that's a little bit more efficient than using an {@code ArrayList<LocalContext>}. 
 * 
 * @since 2.3.24
 */
final class LocalContextStack {

    private LocalContext[] buffer = new LocalContext[8];
    private int size;

    void push(LocalContext localContext) {
        final int newSize = ++size;
        LocalContext[] buffer = this.buffer;
        if (buffer.length < newSize) {
            final LocalContext[] newBuffer = new LocalContext[newSize * 2];
            for (int i = 0; i < buffer.length; i++) {
                newBuffer[i] = buffer[i];
            }
            buffer = newBuffer;
            this.buffer = newBuffer;
        }
        buffer[newSize - 1] = localContext;
    }

    void pop() {
        buffer[--size] = null;
    }

    public LocalContext get(int index) {
        return buffer[index];
    }
    
    public int size() {
        return size;
    }

}
