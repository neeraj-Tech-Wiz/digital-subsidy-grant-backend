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
            int actualScore,
            int maxScore,
            double grantAmount) {

        double scorePercentage = 0.0;
        if (maxScore > 0) {
            scorePercentage = ((double) actualScore / maxScore) * 100;
        }

        // ==========================================
        // SIMPLE ROUTE
        //
        // Score Percentage >= 90%
        // ==========================================

        if (scorePercentage >= 90) {
            return VerificationRoute.SIMPLE;
        }


        // ==========================================
        // STANDARD ROUTE
        //
        // Score Percentage < 90%
        // ==========================================

        return VerificationRoute.STANDARD;
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
            default ->
                    null;
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
            
            default ->
                    "Legacy or undetermined verification route";
        };
    }
}