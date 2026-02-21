package org.tampavolunteers.cli;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.tampavolunteers.model.Opportunity;
import org.tampavolunteers.model.Organization;
import org.tampavolunteers.model.User;
import org.tampavolunteers.model.UserStatus;
import org.tampavolunteers.repository.OpportunityRepository;
import org.tampavolunteers.repository.OrganizationRepository;
import org.tampavolunteers.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * CLI tool for admin operations. Activated with the "cli" Spring profile.
 *
 * Usage:
 *   ./scripts/add-super-admin.sh <email>
 *   ./scripts/remove-super-admin.sh <email>
 *   ./scripts/list-admins.sh
 *   ./scripts/list-users.sh
 *   ./scripts/seed-volunteers.sh <count>
 *   ./scripts/seed-organizations.sh <count>
 *   ./scripts/seed-opportunities.sh <count>
 */
@Component
@Profile("cli")
@RequiredArgsConstructor
public class AdminCLI implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OpportunityRepository opportunityRepository;

    private static final Random RNG = new Random();

    // --- fake data pools ---

    private static final String[] FIRST_NAMES = {
        "James","Mary","John","Patricia","Robert","Jennifer","Michael","Linda",
        "William","Barbara","David","Elizabeth","Richard","Susan","Joseph","Jessica",
        "Thomas","Sarah","Charles","Karen","Christopher","Lisa","Daniel","Nancy",
        "Matthew","Betty","Anthony","Margaret","Mark","Sandra","Donald","Ashley",
        "Steven","Dorothy","Paul","Kimberly","Andrew","Emily","Kenneth","Donna"
    };

    private static final String[] LAST_NAMES = {
        "Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis",
        "Rodriguez","Martinez","Hernandez","Lopez","Gonzalez","Wilson","Anderson",
        "Thomas","Taylor","Moore","Jackson","Martin","Lee","Perez","Thompson",
        "White","Harris","Sanchez","Clark","Ramirez","Lewis","Robinson","Walker",
        "Young","Allen","King","Wright","Scott","Torres","Nguyen","Hill","Flores"
    };

    private static final String[] ORG_PREFIXES = {
        "Tampa Bay","Sunshine","Gulf Coast","Bay Area","Hillsborough","Pinellas",
        "Florida","Bayshore","Riverwalk","Harbor"
    };

    private static final String[] ORG_SUFFIXES = {
        "Food Bank","Community Center","Animal Rescue","Youth Mentors","Senior Services",
        "Habitat Build","Literacy Council","Health Coalition","Environmental Alliance",
        "Arts Foundation"
    };

    private static final String[] STREETS = {
        "100 N Ashley Dr","200 E Kennedy Blvd","301 W Platt St","450 S MacDill Ave",
        "700 N Franklin St","1200 E Busch Blvd","555 Bayshore Blvd","820 N Dale Mabry Hwy",
        "1010 N Howard Ave","330 E Fowler Ave"
    };

    private static final String[] ZIPS = {
        "33601","33602","33603","33604","33605","33606","33607","33609","33610","33611"
    };

    private static final String[] BIOS = {
        "Passionate community member eager to make a difference.",
        "Experienced volunteer with a background in healthcare.",
        "Retired teacher looking to give back to the local community.",
        "Recent college graduate interested in nonprofit work.",
        "Parent of two who volunteers on weekends to help local families.",
        "Avid runner who organizes charity 5K events.",
        "Tech professional using skills for social good.",
        "Lifelong Tampa resident committed to improving the Bay Area.",
        "Social worker by trade, volunteer by heart.",
        "Small business owner supporting local causes."
    };

    private static final String[] OPP_TITLES = {
        "Community Garden Clean-Up","Food Pantry Distribution","Senior Center Bingo Night",
        "Youth Reading Program","Dog Walking at Animal Shelter","Beach Clean-Up Day",
        "After-School Tutoring","Habitat Home Build Day","Blood Drive Support",
        "Park Restoration Project","Soup Kitchen Meal Service","Coat Drive Collection",
        "Computer Skills Workshop","Community Health Fair","Trail Maintenance Day",
        "Holiday Gift Wrapping","Literacy Tutoring Session","Environmental Survey",
        "Fundraiser Gala Help","Neighborhood Safety Walk"
    };

    private static final String[] OPP_DESCS = {
        "Join us for a hands-on volunteer day helping our local community.",
        "Support families in need by assisting with food and supply distribution.",
        "Spend a few hours brightening the day of elderly residents.",
        "Help children develop essential reading and writing skills.",
        "Make a tangible impact on the environment in just a few hours.",
        "Work alongside skilled tradespeople to build affordable housing.",
        "Assist healthcare professionals in delivering community health services.",
        "Help collect and sort donated items for community members in need.",
        "Use your skills to educate and empower underserved residents.",
        "Contribute to a safer, cleaner, and more vibrant neighborhood."
    };

    // -------------------------------------------------------------------------

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "add-super-admin" -> {
                    if (args.length < 2) {
                        System.err.println("Usage: add-super-admin <email>");
                        System.exit(1);
                    }
                    addSuperAdmin(args[1]);
                }
                case "remove-super-admin" -> {
                    if (args.length < 2) {
                        System.err.println("Usage: remove-super-admin <email>");
                        System.exit(1);
                    }
                    removeSuperAdmin(args[1]);
                }
                case "list-admins" -> listAdmins();
                case "list-users" -> {
                    boolean all = args.length > 1 && "--all".equals(args[1]);
                    listUsers(all);
                }
                case "seed-volunteers"    -> seedVolunteers(parseCount(args, "seed-volunteers"));
                case "seed-organizations" -> seedOrganizations(parseCount(args, "seed-organizations"));
                case "seed-opportunities" -> seedOpportunities(parseCount(args, "seed-opportunities"));
                default -> {
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }

        System.exit(0);
    }

    // -------------------------------------------------------------------------
    // Admin commands
    // -------------------------------------------------------------------------

    private void addSuperAdmin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if (user.getRole() == User.UserRole.SUPER_ADMIN) {
            System.out.println("User " + email + " is already a SUPER_ADMIN.");
            return;
        }

        user.setRole(User.UserRole.SUPER_ADMIN);
        userRepository.save(user);
        System.out.println("SUCCESS: " + email + " is now a SUPER_ADMIN.");
    }

    private void removeSuperAdmin(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        if (user.getRole() != User.UserRole.SUPER_ADMIN) {
            System.out.println("User " + email + " is not a SUPER_ADMIN.");
            return;
        }

        // Protect against removing the last super admin
        long superAdminCount = userRepository.countByRole(User.UserRole.SUPER_ADMIN);
        if (superAdminCount <= 1) {
            throw new IllegalStateException(
                    "Cannot remove the last SUPER_ADMIN. Add another SUPER_ADMIN first.");
        }

        user.setRole(User.UserRole.ADMIN);
        userRepository.save(user);
        System.out.println("SUCCESS: " + email + " has been downgraded to ADMIN.");
    }

    private void listAdmins() {
        List<User> admins = userRepository.findByRole(User.UserRole.ADMIN);
        List<User> superAdmins = userRepository.findByRole(User.UserRole.SUPER_ADMIN);

        System.out.println("\n=== SUPER_ADMIN Users ===");
        if (superAdmins.isEmpty()) {
            System.out.println("  (none)");
        } else {
            superAdmins.forEach(u -> System.out.printf("  [%d] %s %s <%s>%n",
                    u.getId(), u.getFirstName(), u.getLastName(), u.getEmail()));
        }

        System.out.println("\n=== ADMIN Users ===");
        if (admins.isEmpty()) {
            System.out.println("  (none)");
        } else {
            admins.forEach(u -> System.out.printf("  [%d] %s %s <%s>%n",
                    u.getId(), u.getFirstName(), u.getLastName(), u.getEmail()));
        }
        System.out.println();
    }

    private void listUsers(boolean all) {
        List<User> users = all
                ? userRepository.findAll()
                : userRepository.findByRole(User.UserRole.VOLUNTEER);

        System.out.printf("%n=== Users (%s) ===%n", all ? "all" : "volunteers only");
        System.out.printf("%-6s %-30s %-20s %-20s %-15s%n",
                "ID", "Email", "First Name", "Last Name", "Role");
        System.out.println("-".repeat(95));

        users.forEach(u -> System.out.printf("%-6d %-30s %-20s %-20s %-15s%n",
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getRole().name()));
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Seed commands
    // -------------------------------------------------------------------------

    private void seedVolunteers(int count) {
        System.out.printf("Seeding %d volunteer(s)...%n", count);
        for (int i = 0; i < count; i++) {
            String firstName = pick(FIRST_NAMES);
            String lastName  = pick(LAST_NAMES);
            String email     = uniqueEmail(firstName, lastName);

            User u = new User();
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setRole(User.UserRole.VOLUNTEER);
            u.setAuthProvider(User.AuthProvider.LOCAL);
            u.setUserStatus(UserStatus.VOLUNTEER);
            u.setIsPublic(RNG.nextBoolean());
            u.setBio(pick(BIOS));
            u.setPhone(fakePhone());

            userRepository.save(u);
            System.out.printf("  [%d/%d] %s %s <%s>%n", i + 1, count, firstName, lastName, email);
        }
        System.out.printf("Done. %d volunteer(s) created.%n", count);
    }

    private void seedOrganizations(int count) {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new IllegalStateException(
                    "No users in the database. Run seed-volunteers first.");
        }

        System.out.printf("Seeding %d organization(s)...%n", count);
        for (int i = 0; i < count; i++) {
            User owner = users.get(RNG.nextInt(users.size()));
            String prefix = pick(ORG_PREFIXES);
            String suffix = pick(ORG_SUFFIXES);
            String name   = prefix + " " + suffix;
            String slug   = name.toLowerCase().replaceAll("[^a-z]", "");

            Organization org = new Organization();
            org.setName(name);
            org.setDescription("A Tampa Bay nonprofit dedicated to " + suffix.toLowerCase() + " in the community.");
            org.setContactEmail("info+" + RNG.nextInt(10000) + "@" + slug + ".org");
            org.setContactPhone(fakePhone());
            org.setWebsite("https://www." + slug + ".org");
            org.setStreet(pick(STREETS));
            org.setCity("Tampa");
            org.setState("FL");
            org.setZip(pick(ZIPS));
            org.setVerified(RNG.nextInt(3) > 0); // ~67% verified
            org.setUser(owner);
            org.setSeeded(true);

            organizationRepository.save(org);
            System.out.printf("  [%d/%d] %s (owner: %s)%n", i + 1, count, name, owner.getEmail());
        }
        System.out.printf("Done. %d organization(s) created.%n", count);
    }

    private void seedOpportunities(int count) {
        List<Organization> orgs = organizationRepository.findAll();
        if (orgs.isEmpty()) {
            throw new IllegalStateException(
                    "No organizations in the database. Run seed-organizations first.");
        }

        System.out.printf("Seeding %d opportunit%s...%n", count, count == 1 ? "y" : "ies");
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            Organization org    = orgs.get(RNG.nextInt(orgs.size()));
            int daysOut         = 1 + RNG.nextInt(90);
            LocalDateTime start = now.plusDays(daysOut).withHour(8 + RNG.nextInt(8)).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end   = start.plusHours(2 + RNG.nextInt(4));
            int slots           = 5 + RNG.nextInt(46);

            Opportunity opp = new Opportunity();
            opp.setOrganization(org);
            opp.setTitle(pick(OPP_TITLES));
            opp.setDescription(pick(OPP_DESCS));
            opp.setStartDateTime(start);
            opp.setEndDateTime(end);
            opp.setStreet(pick(STREETS));
            opp.setCity("Tampa");
            opp.setState("FL");
            opp.setZip(pick(ZIPS));
            opp.setSlotsAvailable(slots);
            opp.setSlotsFilled(0);
            opp.setStatus(Opportunity.OpportunityStatus.PUBLISHED);
            opp.setSeeded(true);

            opportunityRepository.save(opp);
            System.out.printf("  [%d/%d] \"%s\" @ %s — %s, %d slot(s)%n",
                    i + 1, count, opp.getTitle(), org.getName(), start.toLocalDate(), slots);
        }
        System.out.printf("Done. %d opportunit%s created.%n", count, count == 1 ? "y" : "ies");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int parseCount(String[] args, String command) {
        if (args.length < 2) {
            System.err.println("Usage: " + command + " <count>");
            System.exit(1);
        }
        try {
            int n = Integer.parseInt(args[1]);
            if (n < 1) throw new IllegalArgumentException("Count must be >= 1, got: " + n);
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Count must be an integer, got: " + args[1]);
        }
    }

    private String uniqueEmail(String firstName, String lastName) {
        String base = firstName.toLowerCase() + "." + lastName.toLowerCase();
        String email;
        do {
            email = base + "." + RNG.nextInt(100_000) + "@example.com";
        } while (userRepository.findByEmail(email).isPresent());
        return email;
    }

    private String fakePhone() {
        return String.format("(813) %03d-%04d", RNG.nextInt(1000), RNG.nextInt(10000));
    }

    private <T> T pick(T[] arr) {
        return arr[RNG.nextInt(arr.length)];
    }

    private void printUsage() {
        System.out.println("""
                Tampa Volunteers Admin CLI

                Commands:
                  add-super-admin <email>      Promote user to SUPER_ADMIN
                  remove-super-admin <email>   Downgrade SUPER_ADMIN to ADMIN
                  list-admins                  List all ADMIN and SUPER_ADMIN users
                  list-users [--all]           List users (default: volunteers only)
                  seed-volunteers <N>          Create N fake volunteer accounts
                  seed-organizations <N>       Create N fake organizations
                  seed-opportunities <N>       Create N fake published opportunities
                """);
    }
}
