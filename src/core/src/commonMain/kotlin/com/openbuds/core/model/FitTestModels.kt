package com.openbuds.core.model

enum class FitTestStatus {
    IDLE,
    TESTING,
    COMPLETED
}

enum class FitResult {
    UNKNOWN,
    GOOD,
    POOR
}

data class FitTestResult(
    val status: FitTestStatus = FitTestStatus.IDLE,
    val leftResult: FitResult = FitResult.UNKNOWN,
    val rightResult: FitResult = FitResult.UNKNOWN
)
