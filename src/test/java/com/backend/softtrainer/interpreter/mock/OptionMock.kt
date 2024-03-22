package mock

import com.backend.softtrainer.interpreter.Option

val correctTrueOption = Option(
    text = "qw",
    isCorrected = true,
    isSelected = true,
)

val correctFalseOption = Option(
    text = "qw",
    isCorrected = false,
    isSelected = false,
)

val incorrectTrueOption = Option(
    text = "qw",
    isCorrected = true,
    isSelected = false,
)

val incorrectFalseOption = Option(
    text = "qw",
    isCorrected = false,
    isSelected = true,
)