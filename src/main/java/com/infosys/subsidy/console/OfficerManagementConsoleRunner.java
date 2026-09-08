package com.infosys.subsidy.console;

import com.infosys.subsidy.dto.CreateOfficerRequest;
import com.infosys.subsidy.dto.LoginRequest;
import com.infosys.subsidy.dto.OfficerResponse;
import com.infosys.subsidy.dto.AuthResponse;
import com.infosys.subsidy.enums.UserRole;
import com.infosys.subsidy.service.AdminOfficerService;
import com.infosys.subsidy.service.AuthService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
public class OfficerManagementConsoleRunner {

    private final AdminOfficerService adminOfficerService;
    private final AuthService authService;

    public OfficerManagementConsoleRunner(AdminOfficerService adminOfficerService, AuthService authService) {
        this.adminOfficerService = adminOfficerService;
        this.authService = authService;
    }

    public void showOfflineMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("OFFICER MANAGEMENT MENU");
            System.out.println("========================================");
            System.out.println("1. Create Officer (Admin Only)");
            System.out.println("2. View All Officers");
            System.out.println("3. View Officer By ID");
            System.out.println("4. Activate Officer");
            System.out.println("5. Deactivate Officer");
            System.out.println("6. Test Officer Login (Sets Active Session)");
            System.out.println("7. Logout (Clears Active Session)");
            System.out.println("0. Return");
            System.out.println("========================================");
            System.out.print("Enter your choice: ");

            String input = scanner.nextLine();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice.");
                continue;
            }

            switch (choice) {
                case 1 -> createOfficer(scanner);
                case 2 -> viewAllOfficers();
                case 3 -> viewOfficerById(scanner);
                case 4 -> activateOfficer(scanner);
                case 5 -> deactivateOfficer(scanner);
                case 6 -> testOfficerLogin(scanner);
                case 7 -> logout();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void ensureAdminPrivileges() {
        // Since console runs without HTTP, we mock the role in the context solely for demo 
        // to bypass the Service's internal check. Real endpoint restricts using SecurityConfig.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@console.com", null, 
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void createOfficer(Scanner scanner) {
        ensureAdminPrivileges();
        try {
            CreateOfficerRequest request = new CreateOfficerRequest();
            System.out.print("Enter name: ");
            request.setName(scanner.nextLine());
            System.out.print("Enter email: ");
            request.setEmail(scanner.nextLine());
            System.out.print("Enter password: ");
            request.setPassword(scanner.nextLine());
            System.out.println("Available Roles: LEVEL_1_OFFICER, LEVEL_2_OFFICER, LEVEL_3_OFFICER, FINAL_APPROVAL_OFFICER");
            System.out.print("Enter Role: ");
            request.setRole(UserRole.valueOf(scanner.nextLine().toUpperCase()));

            OfficerResponse response = adminOfficerService.createOfficer(request);
            System.out.println("Successfully created officer: ID=" + response.getId() + " Role=" + response.getRole());
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }

    private void viewAllOfficers() {
        ensureAdminPrivileges();
        var officers = adminOfficerService.getAllOfficers();
        officers.forEach(o -> System.out.println(o.getId() + " | " + o.getName() + " | " + o.getEmail() + " | " + o.getRole() + " | Active: " + o.isActive()));
    }

    private void viewOfficerById(Scanner scanner) {
        ensureAdminPrivileges();
        System.out.print("Enter ID: ");
        try {
            Long id = Long.parseLong(scanner.nextLine());
            var o = adminOfficerService.getOfficerById(id);
            System.out.println(o.getId() + " | " + o.getName() + " | " + o.getEmail() + " | " + o.getRole() + " | Active: " + o.isActive());
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }

    private void activateOfficer(Scanner scanner) {
        ensureAdminPrivileges();
        System.out.print("Enter ID: ");
        try {
            Long id = Long.parseLong(scanner.nextLine());
            adminOfficerService.activateOfficer(id);
            System.out.println("SUCCESSFULLY ACTIVATED.");
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }

    private void deactivateOfficer(Scanner scanner) {
        ensureAdminPrivileges();
        System.out.print("Enter ID: ");
        try {
            Long id = Long.parseLong(scanner.nextLine());
            adminOfficerService.deactivateOfficer(id);
            System.out.println("SUCCESSFULLY DEACTIVATED.");
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }

    private void testOfficerLogin(Scanner scanner) {
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();
        System.out.print("Enter Password: ");
        String pass = scanner.nextLine();

        try {
            LoginRequest req = new LoginRequest();
            req.setEmail(email);
            req.setPassword(pass);
            
            AuthResponse response = authService.login(req);
            System.out.println("SUCCESSFUL LOGIN. ROLE: " + response.getRole());

            // Set session so next tasks use it
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + response.getRole()));
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            System.out.println("LOGIN FAILED: " + e.getMessage());
        }
    }

    private void logout() {
        SecurityContextHolder.clearContext();
        System.out.println("CLEARED SECURE SESSION. LOGGED OUT.");
    }
}
