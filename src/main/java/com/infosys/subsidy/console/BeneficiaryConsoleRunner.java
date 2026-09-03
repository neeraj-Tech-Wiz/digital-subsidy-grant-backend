//package com.infosys.subsidy.console;
//
//import com.infosys.subsidy.entity.Beneficiary;
//import com.infosys.subsidy.repository.BeneficiaryRepository;
//import org.springframework.context.annotation.Profile;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//import java.util.NoSuchElementException;
//import java.util.Scanner;
//
//@Component
//@Profile("!test")
//public class BeneficiaryConsoleRunner implements CommandLineRunner {
//
//    private final BeneficiaryRepository beneficiaryRepository;
//
//    public BeneficiaryConsoleRunner(BeneficiaryRepository beneficiaryRepository) {
//        this.beneficiaryRepository = beneficiaryRepository;
//    }
//
//    @Override
//    public void run(String... args) {
//
//        if (System.console() == null && System.getProperty("interactive") == null) {
//            return;
//        }
//
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.println();
//        System.out.println("======================================");
//        System.out.println("   GOVERNMENT SCHEME REGISTRATION");
//        System.out.println("======================================");
//
//        String name = getName(scanner);
//        int age = getAge(scanner);
//        String aadhaarNumber = getAadhaar(scanner);
//        String mobileNumber = getMobile(scanner);
//        String email = getEmail(scanner);
//        String governmentScheme = getScheme(scanner);
//
//        Beneficiary beneficiary = new Beneficiary();
//
//        beneficiary.setName(name);
//        beneficiary.setAge(age);
//        beneficiary.setAadhaarNumber(aadhaarNumber);
//        beneficiary.setMobileNumber(mobileNumber);
//        beneficiary.setEmail(email);
//        beneficiary.setGovernmentScheme(governmentScheme);
//
//        try {
//
//            // Check duplicate Aadhaar
//            if (beneficiaryRepository.existsByAadhaarNumber(aadhaarNumber)) {
//
//                System.out.println();
//                System.out.println("ERROR: Aadhaar number already exists.");
//                return;
//            }
//
//            // Check duplicate mobile
//            if (beneficiaryRepository.existsByMobileNumber(mobileNumber)) {
//
//                System.out.println();
//                System.out.println("ERROR: Mobile number already exists.");
//                return;
//            }
//
//            // Save beneficiary to PostgreSQL
//            Beneficiary savedBeneficiary =
//                    beneficiaryRepository.save(beneficiary);
//
//            // Retrieve the saved beneficiary from PostgreSQL
//            Beneficiary storedBeneficiary =
//                    beneficiaryRepository.findById(savedBeneficiary.getId())
//                            .orElseThrow(() ->
//                                    new RuntimeException(
//                                            "Beneficiary was not found in database"
//                                    )
//                            );
//
//            // Display success message
//            System.out.println();
//            System.out.println("======================================");
//            System.out.println("       REGISTRATION SUCCESSFUL");
//            System.out.println("======================================");
//
//            System.out.println();
//            System.out.println("Data retrieved from PostgreSQL:");
//            System.out.println();
//
//            System.out.println(
//                    "ID                : "
//                            + storedBeneficiary.getId()
//            );
//
//            System.out.println(
//                    "Name              : "
//                            + storedBeneficiary.getName()
//            );
//
//            System.out.println(
//                    "Age               : "
//                            + storedBeneficiary.getAge()
//            );
//
//            System.out.println(
//                    "Aadhaar Number    : "
//                            + storedBeneficiary.getAadhaarNumber()
//            );
//
//            System.out.println(
//                    "Mobile Number     : "
//                            + storedBeneficiary.getMobileNumber()
//            );
//
//            System.out.println(
//                    "Email             : "
//                            + storedBeneficiary.getEmail()
//            );
//
//            System.out.println(
//                    "Government Scheme : "
//                            + storedBeneficiary.getGovernmentScheme()
//            );
//
//            System.out.println();
//            System.out.println("======================================");
//
//        } catch (Exception e) {
//
//            System.out.println();
//            System.out.println("ERROR: Unable to register beneficiary.");
//            System.out.println("Reason: " + e.getMessage());
//        }
//
//        scanner.close();
//    }
//
//    // ==============================
//    // NAME VALIDATION
//    // ==============================
//
//    private String getName(Scanner scanner) {
//
//        while (true) {
//
//            System.out.print("Enter Name: ");
//
//            String name = scanner.nextLine().trim();
//
//            if (name.isEmpty()) {
//
//                System.out.println(
//                        "ERROR: Name cannot be empty."
//                );
//
//                continue;
//            }
//
//            if (!name.matches("[a-zA-Z ]+")) {
//
//                System.out.println(
//                        "ERROR: Name must contain only letters and spaces."
//                );
//
//                continue;
//            }
//
//            return name;
//        }
//    }
//
//    // ==============================
//    // AGE VALIDATION
//    // ==============================
//
//    private int getAge(Scanner scanner) {
//
//        while (true) {
//
//            System.out.print("Enter Age: ");
//
//            String input = scanner.nextLine().trim();
//
//            try {
//
//                int age = Integer.parseInt(input);
//
//                if (age < 0) {
//
//                    System.out.println(
//                            "ERROR: Age cannot be negative."
//                    );
//
//                    continue;
//                }
//
//                if (age > 150) {
//
//                    System.out.println(
//                            "ERROR: Please enter a valid age."
//                    );
//
//                    continue;
//                }
//
//                return age;
//
//            } catch (NumberFormatException e) {
//
//                System.out.println(
//                        "ERROR: Age must contain digits only."
//                );
//            }
//        }
//    }
//
//    // ==============================
//    // AADHAAR VALIDATION
//    // ==============================
//
//    private String getAadhaar(Scanner scanner) {
//
//        while (true) {
//
//            System.out.print("Enter Aadhaar Number: ");
//
//            String aadhaar = scanner.nextLine().trim();
//
//            if (!aadhaar.matches("\\d{12}")) {
//
//                System.out.println(
//                        "ERROR: Aadhaar must contain exactly 12 digits."
//                );
//
//                continue;
//            }
//
//            return aadhaar;
//        }
//    }
//
//    // ==============================
//    // MOBILE VALIDATION
//    // ==============================
//
//    private String getMobile(Scanner scanner) {
//
//        while (true) {
//
//            System.out.print("Enter Mobile Number: ");
//
//            String mobile = scanner.nextLine().trim();
//
//            if (!mobile.matches("\\d{10}")) {
//
//                System.out.println(
//                        "ERROR: Mobile number must contain exactly 10 digits."
//                );
//
//                continue;
//            }
//
//            return mobile;
//        }
//    }
//
//
//
//    private String getEmail(Scanner scanner) {
//
//        while (true) {
//
//            System.out.print("Enter Email: ");
//
//            String email = scanner.nextLine().trim();
//
//            if (!email.matches(
//                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
//            )) {
//
//                System.out.println(
//                        "ERROR: Please enter a valid email address."
//                );
//
//                continue;
//            }
//
//            return email;
//        }
//    }
//
//
//    private String getScheme(Scanner scanner) {
//
//        while (true) {
//
//            System.out.print("Enter Government Scheme: ");
//
//            String scheme = scanner.nextLine().trim();
//
//            if (scheme.isEmpty()) {
//
//                System.out.println(
//                        "ERROR: Government scheme cannot be empty."
//                );
//
//                continue;
//            }
//
//            return scheme;
//        }
//    }
//}