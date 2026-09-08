package com.infosys.subsidy.service;

import com.infosys.subsidy.enums.VerificationLevel;
import com.infosys.subsidy.enums.VerificationRoute;
import org.springframework.stereotype.Service;

@Service
public class VerificationRoutingService {

    // =====================================================
    // DETERMINE VERIFICATION ROUTE
    // BASED ON ELIGIBILITY SCORE + GRANT AMOUNT
    // =====================================================

    public VerificationRoute determineRoute(
            int eligibilityScore,
            double grantAmount) {

        // ==========================================
        // SIMPLE ROUTE
        //
        // Score >= 90
        // AND Grant <= 50,000
        //
        // LEVEL_1 → FINAL_APPROVAL
        // ==========================================

        if (eligibilityScore >= 90
                && grantAmount <= 50000) {

            return VerificationRoute.SIMPLE;
        }


        // ==========================================
        // STANDARD ROUTE
        //
        // Score >= 75
        // AND Grant <= 2,00,000
        //
        // LEVEL_1 → LEVEL_2 → FINAL_APPROVAL
        // ==========================================

        if (eligibilityScore >= 75
                && grantAmount <= 200000) {

            return VerificationRoute.STANDARD;
        }


        // ==========================================
        // HIGH VALUE ROUTE
        //
        // LEVEL_1 → LEVEL_2 → LEVEL_3
        // → FINAL_APPROVAL
        // ==========================================

        return VerificationRoute.HIGH_VALUE;
    }


    // =====================================================
    // GET NEXT LEVEL BASED ON ROUTE
    // =====================================================

    public VerificationLevel getNextLevel(
            VerificationLevel currentLevel,
            VerificationRoute route) {

        if (currentLevel == null || route == null) {
            return null;
        }


        return switch (route) {

            // ======================================
            // SIMPLE
            //
            // LEVEL_1 → FINAL_APPROVAL
            // ======================================

            case SIMPLE ->

                    switch (currentLevel) {

                        case LEVEL_1 ->
                                VerificationLevel.FINAL_APPROVAL;

                        default ->
                                null;
                    };


            // ======================================
            // STANDARD
            //
            // LEVEL_1 → LEVEL_2 → FINAL_APPROVAL
            // ======================================

            case STANDARD ->

                    switch (currentLevel) {

                        case LEVEL_1 ->
                                VerificationLevel.LEVEL_2;

                        case LEVEL_2 ->
                                VerificationLevel.FINAL_APPROVAL;

                        default ->
                                null;
                    };


            // ======================================
            // HIGH VALUE
            //
            // LEVEL_1 → LEVEL_2 → LEVEL_3
            // → FINAL_APPROVAL
            // ======================================

            case HIGH_VALUE ->

                    switch (currentLevel) {

                        case LEVEL_1 ->
                                VerificationLevel.LEVEL_2;

                        case LEVEL_2 ->
                                VerificationLevel.LEVEL_3;

                        case LEVEL_3 ->
                                VerificationLevel.FINAL_APPROVAL;

                        default ->
                                null;
                    };
        };
    }


    // =====================================================
    // GET ROUTE DESCRIPTION
    // =====================================================

    public String getRouteDescription(
            VerificationRoute route) {

        if (route == null) {
            return "No verification route assigned";
        }


        return switch (route) {

            case SIMPLE ->
                    "Simple verification: Level 1 → Final Approval";

            case STANDARD ->
                    "Standard verification: Level 1 → Level 2 → Final Approval";

            case HIGH_VALUE ->
                    "High-value verification: Level 1 → Level 2 → Level 3 → Final Approval";
        };
    }
}