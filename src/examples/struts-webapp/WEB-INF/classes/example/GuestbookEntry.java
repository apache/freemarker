package example;

public class GuestbookEntry {
    private String name;
    private String email;
    private String message;

    public GuestbookEntry(String name, String email, String message) {
        this.name = name;
        this.email = email;
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }

}
