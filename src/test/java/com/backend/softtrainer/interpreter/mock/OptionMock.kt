package mock

import Option

val correctTrueOption = Option(
    id = "0",
    text = "qw",
    isCorrected = true,
    isSelected = true,
)

val correctFalseOption = Option(
    id = "0",
    text = "qw",
    isCorrected = false,
    isSelected = false,
)

val incorrectTrueOption = Option(
    id = "0",
    text = "qw",
    isCorrected = true,
    isSelected = false,
)

val incorrectFalseOption = Option(
    id = "0",
    text = "qw",
    isCorrected = false,
    isSelected = true,
)