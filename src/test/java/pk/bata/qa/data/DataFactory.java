package pk.bata.qa.data;

import pk.bata.qa.core.Config;

import java.util.Random;
import java.util.UUID;

/**
 * Generates a different representative value for every run (equivalence-class representatives).
 * Everything is synthetic: no real personal or payment data.
 */
public final class DataFactory {
    private DataFactory() {}

    private static final long SEED = System.nanoTime();
    private static final String[] FIRST = {"Ayesha", "Hamza", "Zainab", "Bilal", "Hira", "Usman", "Sana", "Danish", "Maryam", "Talha", "Noor", "Faizan"};
    private static final String[] LAST = {"Khan", "Ahmed", "Malik", "Sheikh", "Butt", "Qureshi", "Raza", "Chaudhry", "Mirza", "Siddiqui"};
    private static final String[] AREA = {"Gulberg", "Johar Town", "DHA Phase 5", "Model Town", "Bahria Town", "Garden Town"};
    private static final String[] CITY = {"Lahore", "Karachi", "Islamabad", "Rawalpindi", "Faisalabad", "Multan"};
    private static final String ALNUM = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private static Random rnd(int run) { return new Random(SEED * 31 + run); }
    private static String pick(Random r, String[] a) { return a[r.nextInt(a.length)]; }
    private static String uid() { return UUID.randomUUID().toString().substring(0, 8); }

    public record Customer(String first, String last, String email, String phone, String address, String city, String postal) {}

    public static Customer customer(int run) {
        Random r = rnd(run);
        StringBuilder ph = new StringBuilder("03").append(r.nextInt(5));          // 03[0-4]x xxxxxxx
        for (int i = 0; i < 8; i++) ph.append(r.nextInt(10));
        String addr = "House " + (1 + r.nextInt(400)) + ", Street " + (1 + r.nextInt(40)) + ", " + pick(r, AREA);
        String zip = String.format("%05d", 10000 + r.nextInt(80000));
        return new Customer(pick(r, FIRST), pick(r, LAST), "qa." + uid() + "@" + Config.EMAIL_DOMAIN,
                ph.toString(), addr, pick(r, CITY), zip);
    }

    public static String password(int run) {
        Random r = rnd(run + 7919);
        StringBuilder sb = new StringBuilder("Qa");
        for (int i = 0; i < 10; i++) sb.append(ALNUM.charAt(r.nextInt(ALNUM.length())));
        return sb.append("#9").toString();
    }

    // ---------- invalid classes (rotate so every class is exercised across the runs) ----------

    public record BadReg(String label, String first, String last, String email, String password) {}

    public static BadReg badRegistration(int run) {
        Customer c = customer(run);
        String pw = password(run);
        return switch (run % 6) {
            case 0 -> new BadReg("email_missing_at", c.first(), c.last(), "qa" + uid() + "." + Config.EMAIL_DOMAIN, pw);
            case 1 -> new BadReg("email_empty", c.first(), c.last(), "", pw);
            case 2 -> new BadReg("email_double_at", c.first(), c.last(), "qa" + uid() + "@@" + Config.EMAIL_DOMAIN, pw);
            case 3 -> new BadReg("password_empty", c.first(), c.last(), c.email(), "");
            case 4 -> new BadReg("password_too_short", c.first(), c.last(), c.email(), "a1");
            default -> Config.LOGIN_EMAIL.isEmpty()
                    ? new BadReg("email_with_space", c.first(), c.last(), "qa user" + uid() + "@" + Config.EMAIL_DOMAIN, pw)
                    : new BadReg("email_already_registered", c.first(), c.last(), Config.LOGIN_EMAIL, pw);
        };
    }

    /** Emails that every browser/server must reject, so no verification email is ever sent. */
    public record BadEmail(String label, String email) {}

    public static BadEmail badEmail(int run) {
        String u = "qa" + uid();
        return switch (run % 6) {
            case 0 -> new BadEmail("missing_at", u + "." + Config.EMAIL_DOMAIN);
            case 1 -> new BadEmail("empty", "");
            case 2 -> new BadEmail("double_at", u + "@@" + Config.EMAIL_DOMAIN);
            case 3 -> new BadEmail("contains_space", u + " x@" + Config.EMAIL_DOMAIN);
            case 4 -> new BadEmail("missing_local_part", "@" + Config.EMAIL_DOMAIN);
            default -> new BadEmail("missing_domain", u + "@");
        };
    }

    public record BadLogin(String label, String email, String password) {}

    public static BadLogin badLogin(int run) {
        String rndEmail = "qa." + uid() + "@" + Config.EMAIL_DOMAIN;
        return switch (run % 5) {
            case 0 -> new BadLogin("wrong_password", Config.LOGIN_EMAIL.isEmpty() ? rndEmail : Config.LOGIN_EMAIL, password(run));
            case 1 -> new BadLogin("unregistered_email", rndEmail, password(run));
            case 2 -> new BadLogin("malformed_email", "qa" + uid() + "@@" + Config.EMAIL_DOMAIN, password(run));
            case 3 -> new BadLogin("empty_email", "", password(run));
            default -> new BadLogin("empty_password", rndEmail, "");
        };
    }

    public enum GuestField { EMAIL, FIRST_NAME, ADDRESS, PHONE }

    public record BadGuest(String label, GuestField field, String value) {}

    public static BadGuest badGuest(int run) {
        Random r = new Random(SEED + run);
        return switch (run % 6) {
            case 0 -> new BadGuest("phone_letters", GuestField.PHONE, "abc" + (100 + r.nextInt(800)));
            case 1 -> new BadGuest("phone_too_short", GuestField.PHONE, "03" + (10 + r.nextInt(80)));
            case 2 -> new BadGuest("phone_symbols", GuestField.PHONE, "#$%&*!" + r.nextInt(10));
            case 3 -> new BadGuest("phone_letters_and_digits", GuestField.PHONE, "0301abc" + (1000 + r.nextInt(8000)));
            case 4 -> new BadGuest("phone_five_digits", GuestField.PHONE, "0" + (1000 + r.nextInt(8999)));
            default -> new BadGuest("phone_zeros_short", GuestField.PHONE, "000");
        };
    }

    /** Invalid quantity representatives for the add-to-cart / continue-shopping use case. */
    public static String badQuantity(int run) {
        String[] q = {"0", "-1", "1.5", ""};
        return q[run % q.length];
    }
}
