import Question from "../entity/Question";
import {OnboardingQuestionType} from "../entity/OnboardingQuestionType";

export function getOnboardingQuestion(userId: string) {
    const countQuestion = getCountOnboardingQuestion(userId)
    if (countQuestion < onboardingQuestions.length) {
        return onboardingQuestions[countQuestion]
    } else {
        return null
    }
}

function getCountOnboardingQuestion(userId: string) {
    // Get all onboarding messages
    // select only question
    // return count
    return 0//TODO: Not implement
}

const standartQuestionRange = ["1", "2", "3", "4", "5",]

const onboardingQuestions: Question[] = [
    {
        question: "What is your current profession and role?",
        type: OnboardingQuestionType.OPEN,
    },
    {
        question: "How many years have you worked in that profession?",
        type: OnboardingQuestionType.OPEN,
    },
    {
        question: "How good are you with Communication skill on a scale from 0 to 5?",
        type: OnboardingQuestionType.OPTIONAL,
        options: standartQuestionRange,
    },
    {
        question: "How good are you with Collaboration skill on a scale from 0 to 5?",
        type: OnboardingQuestionType.OPTIONAL,
        options: standartQuestionRange,
    },
    {
        question: "How good are you with Adaptation skill on a scale from 0 to 5?",
        type: OnboardingQuestionType.OPTIONAL,
        options: standartQuestionRange,
    },
    {
        question: "How good are you with Problem-solving skill on a scale from 0 to 5?",
        type: OnboardingQuestionType.OPTIONAL,
        options: standartQuestionRange,
    },
    {
        question: "How good are you with Leadership skill on a scale from 0 to 5?",
        type: OnboardingQuestionType.OPTIONAL,
        options: standartQuestionRange,
    },
    {
        question: "How good are you with Leadership skill on a scale from 0 to 5?",
        type: OnboardingQuestionType.OPTIONAL,
        options: standartQuestionRange,
    },
    {
        question: "How good are you with Leadership skill on a scale from 0 to 5?",
        type: OnboardingQuestionType.MULTISELECT,
        options: standartQuestionRange,
    },
]
