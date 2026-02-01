package org.tampavolunteers.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.tampavolunteers.model.User;
import org.tampavolunteers.repository.UserRepository;

import java.util.List;
import java.util.Scanner;

/**
 * Command-line utility for resetting user passwords and emails.
 *
 * Usage:
 * Run with: --reset-password=true
 * Example: ./mvnw spring-boot:run -Dspring-boot.run.arguments="--reset-password=true"
 */
@Component
@ConditionalOnProperty(name = "reset-password", havingValue = "true")
@RequiredArgsConstructor
public class PasswordResetUtility implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n========================================");
        System.out.println("   PASSWORD RESET UTILITY");
        System.out.println("========================================\n");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nSelect an option:");
            System.out.println("1. List all users");
            System.out.println("2. Reset password by email");
            System.out.println("3. Update email address");
            System.out.println("4. Exit");
            System.out.print("\nEnter your choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    listAllUsers();
                    break;
                case "2":
                    resetPassword(scanner);
                    break;
                case "3":
                    updateEmail(scanner);
                    break;
                case "4":
                    System.out.println("\nExiting utility...");
                    System.exit(0);
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void listAllUsers() {
        List<User> users = userRepository.findAll();

        if (users.isEmpty()) {
            System.out.println("\nNo users found in the database.");
            return;
        }

        System.out.println("\n========================================");
        System.out.println("REGISTERED USERS:");
        System.out.println("========================================");
        System.out.printf("%-5s %-30s %-20s %-20s %-15s%n", "ID", "Email", "First Name", "Last Name", "Role");
        System.out.println("----------------------------------------");

        for (User user : users) {
            System.out.printf("%-5d %-30s %-20s %-20s %-15s%n",
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name());
        }
        System.out.println("========================================\n");
    }

    private void resetPassword(Scanner scanner) {
        System.out.print("\nEnter user email: ");
        String email = scanner.nextLine().trim();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            System.out.println("❌ User with email '" + email + "' not found.");
            return;
        }

        System.out.print("Enter new password: ");
        String newPassword = scanner.nextLine().trim();

        if (newPassword.length() < 8) {
            System.out.println("❌ Password must be at least 8 characters long.");
            return;
        }

        System.out.print("Confirm new password: ");
        String confirmPassword = scanner.nextLine().trim();

        if (!newPassword.equals(confirmPassword)) {
            System.out.println("❌ Passwords do not match.");
            return;
        }

        // Hash and update the password
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        System.out.println("\n✅ Password successfully reset for user: " + email);
        System.out.println("   User can now login with the new password.");
    }

    private void updateEmail(Scanner scanner) {
        System.out.print("\nEnter current email: ");
        String currentEmail = scanner.nextLine().trim();

        User user = userRepository.findByEmail(currentEmail).orElse(null);

        if (user == null) {
            System.out.println("❌ User with email '" + currentEmail + "' not found.");
            return;
        }

        System.out.print("Enter new email: ");
        String newEmail = scanner.nextLine().trim();

        // Check if new email already exists
        if (userRepository.existsByEmail(newEmail)) {
            System.out.println("❌ Email '" + newEmail + "' is already in use.");
            return;
        }

        // Validate email format (basic validation)
        if (!newEmail.contains("@") || !newEmail.contains(".")) {
            System.out.println("❌ Invalid email format.");
            return;
        }

        user.setEmail(newEmail);
        userRepository.save(user);

        System.out.println("\n✅ Email successfully updated!");
        System.out.println("   Old email: " + currentEmail);
        System.out.println("   New email: " + newEmail);
    }
}
